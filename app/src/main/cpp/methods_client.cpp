#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <errno.h>

#define DEFAULT_PORT 12345
#define BUFFER_SIZE 65536

// 读取端口文件
int read_port_file(const char* port_file) {
    FILE* fp = fopen(port_file, "r");
    if (!fp) {
        return -1;
    }

    int port = -1;
    if (fscanf(fp, "%d", &port) != 1) {
        fclose(fp);
        return -1;
    }

    fclose(fp);
    return port;
}

// 连接到服务器
int connect_to_server(int port) {
    int sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) {
        return -1;
    }

    struct sockaddr_in server_addr;
    memset(&server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_port = htons(port);
    server_addr.sin_addr.s_addr = inet_addr("127.0.0.1");

    if (connect(sockfd, (struct sockaddr*)&server_addr, sizeof(server_addr)) < 0) {
        close(sockfd);
        return -1;
    }

    return sockfd;
}

// 交互式模式
void interactive_mode(int port) {
    int sockfd = connect_to_server(port);
    if (sockfd < 0) {
        fprintf(stderr, "无法连接到服务器 (端口: %d)\n", port);
        exit(1);
    }

    // 使用 select 实现双向通信
    fd_set read_fds;
    char buffer[BUFFER_SIZE];
    
    while (1) {
        FD_ZERO(&read_fds);
        FD_SET(sockfd, &read_fds);
        FD_SET(STDIN_FILENO, &read_fds);
        
        int max_fd = (sockfd > STDIN_FILENO) ? sockfd : STDIN_FILENO;
        
        if (select(max_fd + 1, &read_fds, NULL, NULL, NULL) < 0) {
            break;
        }
        
        // 从服务器接收数据
        if (FD_ISSET(sockfd, &read_fds)) {
            ssize_t received = recv(sockfd, buffer, BUFFER_SIZE - 1, 0);
            if (received <= 0) {
                break;
            }
            buffer[received] = '\0';
            printf("%s", buffer);
            fflush(stdout);
        }
        
        // 从stdin读取数据
        if (FD_ISSET(STDIN_FILENO, &read_fds)) {
            if (fgets(buffer, BUFFER_SIZE, stdin) == NULL) {
                break;
            }
            
            // 检查退出命令
            if (strcmp(buffer, "exit\n") == 0 || strcmp(buffer, "quit\n") == 0) {
                break;
            }
            
            // 发送到服务器
            send(sockfd, buffer, strlen(buffer), 0);
        }
    }
    
    close(sockfd);
}

// 自动模式
int auto_mode(int port, const char* command) {
    int sockfd = connect_to_server(port);
    if (sockfd < 0) {
        fprintf(stderr, "无法连接到服务器 (端口: %d)\n", port);
        return 1;
    }

    // 发送命令
    send(sockfd, command, strlen(command), 0);

    // 接收响应
    char buffer[BUFFER_SIZE];
    ssize_t total_received = 0;
    
    while (1) {
        ssize_t received = recv(sockfd, buffer + total_received, 
                                 BUFFER_SIZE - total_received - 1, 0);
        if (received <= 0) {
            break;
        }
        total_received += received;
    }

    buffer[total_received] = '\0';
    printf("%s", buffer);

    close(sockfd);
    return 0;
}

int main(int argc, char* argv[]) {
    int port = -1;
    const char* command = NULL;
    int interactive = 0;

    // 解析命令行参数
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "-p") == 0 || strcmp(argv[i], "--port") == 0) {
            if (i + 1 < argc) {
                port = atoi(argv[++i]);
            }
        } else if (strcmp(argv[i], "-i") == 0 || strcmp(argv[i], "--interactive") == 0) {
            interactive = 1;
        } else if (argv[i][0] != '-') {
            command = argv[i];
        }
    }

    // 如果没有指定端口，从端口文件读取
    if (port <= 0) {
        const char* port_file = "/data/local/tmp/methods/methods_port";
        port = read_port_file(port_file);
        if (port <= 0) {
            fprintf(stderr, "无法读取端口文件，请使用 -p 指定端口\n");
            return 1;
        }
    }

    // 根据模式执行
    if (interactive || command == NULL) {
        interactive_mode(port);
    } else {
        auto_mode(port, command);
    }

    return 0;
}
