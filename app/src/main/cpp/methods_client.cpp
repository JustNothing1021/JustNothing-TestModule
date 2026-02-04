#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cerrno>
#include <ctime>
#include <cstdarg>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#define BUFFER_SIZE 65536

// 调试模式
int debug_mode = 0;

// 调试输出函数
void debug_print(const char* format, ...) {
    if (!debug_mode) return;
    
    va_list args;
    va_start(args, format);
    
    time_t now;
    time(&now);
    struct tm* timeinfo = localtime(&now);
    char timestamp[20];
    strftime(timestamp, sizeof(timestamp), "%H:%M:%S", timeinfo);
    
    fprintf(stderr, "[%s] DEBUG: ", timestamp);
    vfprintf(stderr, format, args);
    fprintf(stderr, "\n");
    
    va_end(args);
}

// 读取端口文件
int read_port_file(const char* port_file) {
    debug_print("尝试读取端口文件: %s", port_file);
    
    FILE* fp = fopen(port_file, "r");
    if (!fp) {
        debug_print("无法打开端口文件: %s (errno: %d)", port_file, errno);
        return -1;
    }

    int port = -1;
    if (fscanf(fp, "%d", &port) != 1) {
        debug_print("端口文件格式错误");
        fclose(fp);
        return -1;
    }

    fclose(fp);
    debug_print("成功读取端口: %d", port);
    return port;
}

// 连接到服务器
int connect_to_server(int port) {
    debug_print("尝试连接到服务器，端口: %d", port);
    
    int sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) {
        debug_print("创建socket失败 (errno: %d)", errno);
        return -1;
    }
    debug_print("socket创建成功，fd: %d", sockfd);

    struct sockaddr_in server_addr{};
    memset(&server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_port = htons(port);
    server_addr.sin_addr.s_addr = inet_addr("127.0.0.1");

    debug_print("尝试连接服务器...");
    if (connect(sockfd, (struct sockaddr*)&server_addr, sizeof(server_addr)) < 0) {
        debug_print("连接失败 (errno: %d)", errno);
        close(sockfd);
        return -1;
    }

    debug_print("连接服务器成功");
    return sockfd;
}

// 交互式模式
void interactive_mode(int port) {
    debug_print("进入交互式模式，端口: %d", port);
    
    int sockfd = connect_to_server(port);
    if (sockfd < 0) {
        fprintf(stderr, "无法连接到服务器 (端口: %d)\n", port);
        exit(1);
    }

    debug_print("交互式模式连接成功");
    
    // 使用 select 实现双向通信
    fd_set read_fds;
    char buffer[BUFFER_SIZE];
    
    while (true) {
        FD_ZERO(&read_fds);
        FD_SET(sockfd, &read_fds);
        FD_SET(STDIN_FILENO, &read_fds);
        
        int max_fd = (sockfd > STDIN_FILENO) ? sockfd : STDIN_FILENO;
        
        debug_print("等待select...");
        if (select(max_fd + 1, &read_fds, nullptr, nullptr, nullptr) < 0) {
            debug_print("select失败 (errno: %d)", errno);
            break;
        }
        
        // 从服务器接收数据
        if (FD_ISSET(sockfd, &read_fds)) {
            ssize_t received = recv(sockfd, buffer, BUFFER_SIZE - 1, 0);
            if (received <= 0) {
                debug_print("服务器断开连接 (received: %zd)", received);
                break;
            }
            // 确保不会溢出
            if (received >= BUFFER_SIZE) {
                received = BUFFER_SIZE - 1;
                debug_print("数据截断，实际接收: %zd字节", received);
            }
            buffer[received] = '\0';
            debug_print("从服务器接收数据: %zd字节", received);
            printf("%s", buffer);
            fflush(stdout);
        }
        
        // 从stdin读取数据
        if (FD_ISSET(STDIN_FILENO, &read_fds)) {
            if (fgets(buffer, BUFFER_SIZE, stdin) == nullptr) {
                debug_print("stdin读取失败");
                break;
            }
            
            debug_print("从stdin读取数据: %s", buffer);
            
            // 检查退出命令
            if (strcmp(buffer, "exit\n") == 0 || strcmp(buffer, "quit\n") == 0) {
                debug_print("收到退出命令");
                break;
            }
            
            // 发送到服务器
            ssize_t sent = send(sockfd, buffer, strlen(buffer), 0);
            debug_print("发送数据到服务器: %zd字节", sent);
        }
    }
    
    debug_print("退出交互式模式");
    close(sockfd);
}

// 自动模式
int auto_mode(int port, const char* command) {
    debug_print("进入自动模式，端口: %d, 命令: %s", port, command);
    
    int sockfd = connect_to_server(port);
    if (sockfd < 0) {
        fprintf(stderr, "无法连接到服务器 (端口: %d)\n", port);
        return 1;
    }

    debug_print("自动模式连接成功");
    
    // 发送命令
    ssize_t sent = send(sockfd, command, strlen(command), 0);
    debug_print("发送命令到服务器: %zd字节", sent);

    // 接收响应
    char buffer[BUFFER_SIZE];
    ssize_t total_received = 0;
    
    debug_print("开始接收响应...");
    while (true) {
        // 检查缓冲区是否已满
        if (total_received >= BUFFER_SIZE - 1) {
            debug_print("缓冲区已满，停止接收");
            break;
        }
        
        ssize_t received = recv(sockfd, buffer + total_received, 
                                 BUFFER_SIZE - total_received - 1, 0);
        if (received <= 0) {
            debug_print("接收完成 (received: %zd)", received);
            break;
        }
        debug_print("接收到数据块: %zd字节", received);
        total_received += received;
    }

    buffer[total_received] = '\0';
    debug_print("总共接收数据: %zd字节", total_received);
    printf("%s", buffer);

    close(sockfd);
    debug_print("自动模式执行完成");
    return 0;
}

int main(int argc, char* argv[]) {
    int port = -1;
    const char* command = nullptr;
    int interactive = 0;

    // 解析命令行参数
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "-p") == 0 || strcmp(argv[i], "--port") == 0) {
            if (i + 1 < argc) {
                port = atoi(argv[++i]);
            }
        } else if (strcmp(argv[i], "-i") == 0 || strcmp(argv[i], "--interactive") == 0) {
            interactive = 1;
        } else if (strcmp(argv[i], "-d") == 0 || strcmp(argv[i], "--debug") == 0) {
            debug_mode = 1;
            debug_print("调试模式已启用");
        } else if (argv[i][0] != '-') {
            command = argv[i];
        }
    }

    // 如果没有指定端口，从端口文件读取
    if (port <= 0) {
        const char* port_file = "/data/local/tmp/methods/methods_port";
        port = read_port_file(port_file);
        if (port <= 0) {
            fprintf(stderr, "无法读取端口文件，请使用-p指定端口\n");
            return 1;
        }
    }

    // 根据模式执行
    if (interactive || command == nullptr) {
        interactive_mode(port);
    } else {
        auto_mode(port, command);
    }

    return 0;
}
