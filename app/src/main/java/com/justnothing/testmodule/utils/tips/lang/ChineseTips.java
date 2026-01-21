package com.justnothing.testmodule.utils.tips.lang;

import com.justnothing.testmodule.utils.tips.SimpleTipCallback;
import com.justnothing.testmodule.utils.tips.SpecialTipCallback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import cn.hutool.core.date.ChineseDate;

public class ChineseTips {

    private static final Random random = new Random(System.currentTimeMillis());

    public static class SpecialTips {
        public static List<SpecialTipCallback> getSpecialTips() {
            List<SpecialTipCallback> specialTips = new ArrayList<>();

            Supplier<String> t = () -> {
                List<String> stringList = List.of(
                        "其实点击按钮可以打开功能（好吧是人都知道）",
                        "事实证明, 不写bat可以让人心情舒畅",
                        "祝你破解顺利！",
                        "其实进bootloader不一定是变砖了",
                        "你曾经历过900E的恐惧吗？",
                        "我完全听不懂，所以，这应该是艺术",
                        "AI太好用了你知道吗",
                        "有的时候不一定要找别人问问题，可以上网搜索",
                        "手表无法开机时可以用超级恢复救砖",
                        "怎么不算是一种公益呢？"
                );
                // Random.nextInt(int, int) -> int 在设备上找不到。。。
                return stringList.get(Math.abs(random.nextInt()) % stringList.size());
            };
            specialTips.add(new SpecialTipCallback(
                    t.get(),
                    0
            ));
            
            specialTips.add(new SpecialTipCallback(
                "今天是元旦节！元旦节快乐！",
                80
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 1 && day == 1;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是植树节，但就算不去植树也可以保护环境，比如随手关灯之类的",
                80
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 3 && day == 12;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "愚人节快乐！",
                80
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 4 && day == 1;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是劳动节，但不管在哪一天都不能忘记劳动者们的贡献！",
                80
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 5 && day == 1;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是青年节 - 属于我们的节日！",
                80
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 5 && day == 4;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是母亲节，不去给她点祝福什么的吗？",
                80
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 5 && day == 12;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "如果你是学生的话，去祝老师们教师节快乐吧！",
                80
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 9 && day == 10;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是圣诞节！Merry Christmas！",
                80
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 12 && day == 25;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "情人节快乐！",
                80
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 2 && day == 14;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是妇女节，祝所有女性节日快乐！",
                80
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 3 && day == 8;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "儿童节！儿童节快乐！",
                80
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 6 && day == 1;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是父亲节，去给他一个祝福吧！",
                80
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 6 && day == 16;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是建党节！希望党带领我们走向伟大复兴！",
                80
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 7 && day == 1;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是建军节，致敬所有军人！",
                80
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 8 && day == 1;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是国庆节，祝祖国繁荣昌盛！",
                80
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 10 && day == 1;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是大年初一！祝全家春节快乐团圆，迎接新的一年！",
                90
            ) {
                @Override
                public boolean shouldShow() {
                    try {
                        Date date = new Date();
                        ChineseDate chineseDate = new ChineseDate(date);
                        return chineseDate.getMonth() == 1 && chineseDate.getDay() == 1;
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是元宵节！",
                90
            ) {
                @Override
                public boolean shouldShow() {
                    try {
                        Date date = new Date();
                        ChineseDate chineseDate = new ChineseDate(date);
                        return chineseDate.getMonth() == 1 && chineseDate.getDay() == 15;
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是中秋节，月圆人团圆！",
                90
            ) {
                @Override
                public boolean shouldShow() {
                    try {
                        Date date = new Date();
                        ChineseDate chineseDate = new ChineseDate(date);
                        return chineseDate.getMonth() == 8 && chineseDate.getDay() == 15;
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "端午安康！（我总不能说快乐吧...）",
                90
            ) {
                @Override
                public boolean shouldShow() {
                    try {
                        Date date = new Date();
                        ChineseDate chineseDate = new ChineseDate(date);
                        return chineseDate.getMonth() == 5 && chineseDate.getDay() == 5;
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是七夕节，节日快乐！",
                90
            ) {
                @Override
                public boolean shouldShow() {
                    try {
                        Date date = new Date();
                        ChineseDate chineseDate = new ChineseDate(date);
                        return chineseDate.getMonth() == 7 && chineseDate.getDay() == 7;
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "是清明节，该探望一下祖先了吧",
                90
            ) {
                @Override
                public boolean shouldShow() {
                    try {
                        Date date = new Date();
                        ChineseDate chineseDate = new ChineseDate(date);
                        return (chineseDate.getMonth() == 3 && chineseDate.getDay() == 7) ||
                               (chineseDate.getMonth() == 4 && chineseDate.getDay() == 8);
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "这么晚了还不睡？已经%d点%d分了，赶紧去睡吧",
                60
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    return hour >= 1 && hour <= 4;
                }
                
                @Override
                public String getContent() {
                    Calendar calendar = Calendar.getInstance();
                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    int minute = calendar.get(Calendar.MINUTE);
                    return String.format(content, hour, minute);
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是云沫的生日！又过一年，估计咱仨约定他的项目还鸽着呢（雾",
                100
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 1 && day == 15;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是NoneColdWind的生日！不说了，我得去和他好好友尽一下",
                100
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 1 && day == 16;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是NAN的生日！请他吃God uses VPN! (bushi)",
                100
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 8 && day == 18;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是JustNothing的生日！（高兴）",
                100
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 9 && day == 11;
                }
            });
            
            specialTips.add(new SpecialTipCallback(
                "今天是PAQ的生日！（虽然已经毕业好几年了但我不会忘的）",
                100
            ) {
                @Override
                public boolean shouldShow() {
                    Calendar calendar = Calendar.getInstance();
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    return month == 10 && day == 21;
                }
            });
            
            return specialTips;
        }
    }
    
    public static class DidYouKnowTips {

        private static int count = 0;
        private static final Map<Integer, SimpleTipCallback> map = new HashMap<>();

        public static Map<Integer, SimpleTipCallback> getDidYouKnowTips() {
            String NAME_JUSTNOTHING = "真的啥也不是啊 (JustNothing)";
            String NAME_NONECOLDWIND = "十三怀旧 (NoneColdWind)";
            String NAME_NAN = "SB1.0 (NAN)";
            String NAME_BARINFXXK = "你哪个省的";
            String NAME_DYD = "DYD";
            String NAME_YUNMO = "云沫";
            clearDidYouKnowTips();

            addDidYouKnowTip(new SimpleTipCallback(
                    "其实这个功能并没有什么用，只是用来消遣的",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "你可以通过修改这个软件的源码，让它变成你的 (这个包没任何混淆，相信你的技术力)",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "在windows中按住Win+R键可以打开运行框，输入powershell并以管理员模式运行，" +
                            "在powershell窗口里面输入wininit就能让电脑蓝屏了（虽然也不知道有什么用）",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "vbs有很多种玩法，比如msgbox就是一个很好的玩法，可以用来骚扰你朋友的电脑（bushi）",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "不，你不知道",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "把Xposed模块部分和应用部分组合起来的时候要小心，" +
                            "因为XposedApi是CompileOnly，如果在应用部分调用了会boom",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "你现在所看到的就是\"你知道吗？\"中的第7条内容",
                    NAME_JUSTNOTHING
            ));


            addDidYouKnowTip(new SimpleTipCallback(
                    """
                            因为这玩意生成是随机的，所以偶尔会有同一个提示出现两三次，\
                            但是如果出现了4次，5次，甚至6次...那你就可以去买彩票了
                            
                            注:
                            连续3次的几率：%.6f%%
                            连续4次的几率：%.6f%%
                            连续5次的几率：%.6f%%
                            """,
                    NAME_JUSTNOTHING
            ) {

                @Override
                public String getContent() {
                    int totalTips = getDidTipCount();
                    double chance3 = 1d / (totalTips * totalTips);
                    double chance4 = chance3 / totalTips;
                    double chance5 = chance4 / totalTips;
                    return String.format(content, chance3 * 100.0d, chance4 * 100.0d, chance5 * 100.0d);
                }
            });

            addDidYouKnowTip(new SimpleTipCallback(
                    "真的啥也不是啊成分复杂，平时会写一些曲子，整整嵌入式开发啥的，游戏上玩MC，" +
                            "王者荣耀（退坑快一年了），蛋仔派对（很诡异对吧），原神（最近回坑了），偶尔编程（比如现在）",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "我刚刚看了我写的，哇，写到第十条了，太不容易了",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "其实这个Xposed模块有配套的Magisk模块，可以添加一个名字叫\"methods\"的命令行，" +
                            "脚本里面的代码执行器执行的其实就是它",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "事实证明，AI还是太好用了（其实是AI忘记上下文前的最后幻想，到后面还是得手敲）",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "在Windows里面的cmd里面输入color f7可以原神启动（不建议晚上用）",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "十三怀旧(\"又名NoneColdWind\")对我写的第一个小提示" +
                            "（\"其实我也不知道该写什么，所以只好写这个了\"）" +
                            "的评价是：听君一席话，如听一席话（?）",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "在Win+R调出的运行框里输入regedit并运行，可以打开注册表编辑器，看看就好，" +
                            "千万不要手贱去删里面重要的东西（别问我怎么知道的）",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "其实只要输入一个指令：adb shell pm uninstall " +
                            "--user 0 com.xtc.i3launcher就能让手表的桌面瘫痪（就是把桌面卸载了），" +
                            "但是可以救，要把比出厂版本桌面版本高的桌面装回去，但还是不建议尝逝",
                    NAME_JUSTNOTHING
            ));


            addDidYouKnowTip(new SimpleTipCallback(
                    "虽然我不知道你是刷了第几次才看到这个，但如果你第一次就看到了，那也算是某种幸运了？",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "一般来说，在使用拓展命令时输入help，--help或者.help可以看到命令的使用教程（一般是英文）",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "十三怀旧成分也很复杂，同时电脑配置不是很好（是根本不行）",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "一般来说，预算低用A卡，预算充足用N卡",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    """
                            推荐性价比配置（800元）：Intel_E3-1231v3+精粤H97M-VH-PLUS\
                            +AMD_RX580-8G+DDR3-1600MHz-8G*2+500WS电源+四铜管散热器
                            
                            （至少对我这种 预 算 充 足 的人来说）
                            """,
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "十三怀旧喜欢的显卡——七彩虹iGame_Gefore_RTX4060Ti_Ultra_W_DOC",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "MC里长按F3+C会有ojng（mojang去掉ma）的彩蛋",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "MC中，地狱是个睡觉的“好”地方",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "第19到31条是十三怀旧写的（\\\\\"....\"//）",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "知周所众，jre和jdk的优化是编程界最好的（bushi）",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "如果大脑未响应，请不要尝逝强制关闭它，因为可能会当场暴毙",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "MC中，loli_pickaxe是个好mod，因为在1.12.2中，它可以使好朋友的电脑蓝屏（会被360拦截）",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "看了一下，Wow，第29条了",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "原神是个好东西，它使我成绩上升了，抗压能力变高了，但同时血压也变高了",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "关于英特尔奔腾E5300+ATI_Radeon_HD_4350_Series" +
                            "（2009年的老设备{十三怀旧的}）玩不了原神但可以玩崩坏3这件事",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "啥也不是有一台已经快%d年的大电脑了，但它还很健在",
                    NAME_JUSTNOTHING
            ) {
                @Override
                public String getContent() {
                    Calendar calendar = Calendar.getInstance();
                    int year = calendar.get(Calendar.YEAR);
                    if (year <= 2015) return "我穿越回" + year + "年了？？";
                    return String.format(Locale.getDefault(), content, year - 2015);
                }
            });

            addDidYouKnowTip(new SimpleTipCallback(
                    "事实证明，我再多看一眼这个脚本就会原地螺旋升天爆炸了（？）",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "早期的我破解时也很智慧，对着电脑连手表，搞了半天没连上还以为是手表坏了" +
                            "（后来才知道是没装驱动）",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    """
                            我、十三怀旧和SB1.0曾经研究过一种很新的加密传输文字的方法用来在考试时作弊\
                            （开玩笑的，但研究是真的）
                            
                            但可惜我们到毕业了也没用过（雾
                            """,
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "我以前把WIN11的时候总感觉有点不适应，可能是因为右键菜单被折叠了，" +
                            "但我又不知道怎么把这玩意关掉，所以把它降回去了",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "原神退了半年多之后体检的时候，我血压收缩压比上学期降了4mmHg，但是十三怀旧刚回原神，" +
                            "他收缩压升了......这证明了什么？",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "现在我在复兴以前的\"你知道吗？\"，还得把那些以前的内容都改成过去式（爆炸）",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "RavenField是一款不错的战地模拟器，但是我弟一般喜欢玩里面的载具，" +
                            "而且总是用载具《冲锋陷阵》，所以我管它叫毁车与坠机了（不过我弟好像挺喜欢这个名字？）",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "这是SB1.0编译的第一条内容",
                    NAME_NAN
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "编这个的所有开发者都玩《原神》",
                    NAME_NAN
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "看你到第43条时，你会发现你被骗了",
                    NAME_NAN
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "其实没有第42条,不信去翻源代码 \n\n(JustNothing: 都编译成apk了不会还去翻吧...)",
                    NAME_NAN
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    """
                            101001110011111 111100101011110 1111111100001100 \
                            101010000101111 101001010101000 1111111100000001
                            
                            （真的啥也不是啊友情翻译：原神，启动！）
                            """,
                    NAME_NAN
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "想当年，文明其体魄，野蛮其精神是十六班的基本特征",
                    NAME_NAN
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    """
                            记得篮球赛的时候，SB1.0看着敌队班级的分数高出自己班十几分时，\
                            手中的学牲会常规记录表（可给敌对班级扣分）越来越紧……
                            
                            （当然，他最后没乱扣分)""",
                    NAME_NAN
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    """
                            十三怀旧每一天都与0和1打交道，16班上的女生总是聊有关0和1的话题
                            
                            （JustNothing：大脑飞速运转......出现错误：java.lang.NullPointerException）
                            """,
                    NAME_NAN
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "SB1.0没有好设备\n" +
                            "（JustNothing：是真的，他甚至把一个沿用3年的电子棋盘当主要设备）",
                    NAME_NAN
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "第40到第48条是sb1.0编写的，除去十三怀旧和你哪个省的写的其他都是真的啥也不是啊写的",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "没有人觉得Java很诡异吗，new返回的东西竟然不是指针（来自C++两年用户转Java的疑惑）\n" +
                            "（NoneColdWind：内存安全，小子）",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "真的啥也不是啊是个fvv",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    """
                            想当年用记事本和古法工艺手搓bat的时候看到"ECHO 处于关闭状态。"或者"命令语法不正确。"的时候\
                            总是会红温很久，现在不用担心了，我们有Undefined Behavior和NullPointerException（boom）
                            
                            （AndroidStudio：亻尔女子，Replace with Object.requireNonNull(...)）
                            """,
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "每日一题，防止你玩电脑玩成sadbee： 33+33=？" ,
                    NAME_BARINFXXK
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "兄弟你有什什什什什什什什什什什什什什什什什什什什什么实力啊" ,
                    NAME_BARINFXXK
            ));

            Supplier<String> s = () -> {
                StringBuilder sb = new StringBuilder("看那里，有一个房子！不，仔细看，那是个");
                for (int i = 0; i < 114; i++) sb.append("\n"); // 防止他们真的懒得翻了
                sb.append("房子!!!!");
                return sb.toString();
            };
            addDidYouKnowTip(new SimpleTipCallback(
                    s.get(),
                    NAME_BARINFXXK
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "其实你每呼吸60秒，寿命就会减少一分钟（那不呼吸就是能永生了）",
                    NAME_BARINFXXK
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "其实真的有114514.com这个网站（不信你试试）",
                    NAME_BARINFXXK
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "其实我一直在思考怎么复现之前的输入特殊内容触发彩蛋",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "其实我们这群写XP模块的也很无助，但凡在早期多创建点线程或者动点文件系统就卡死了，" +
                            "甚至在Binder里面不小心给Parcel给recycle()了都会让系统爆炸（点名批评，让我排查了半天）",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "真的啥也不是啊是个脆皮，可能被各种人事（特指疾病）物打出instakill" +
                            "（瞬杀，说简单点就是原地趋势，但是我现在还活着），但是十三怀旧相反",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    """
                            .....././././../.、。。、、、、不回家好好看过一个一般规划\
                            基本是规划局规划局局工会VG和VG回家办公还会举办
                            
                            （我们以前16班上的某个StringBuilder写的）""",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "YEAH!!!!!!! WE WIN!!!!!!!!!!",
                    "JustNothing现在的班上的某个神秘人1号"
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "我们不说\"受着\"，但可以说：\"忍辱负重\"!",
                    "JustNothing现在的班上的某个神秘人1号"
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "你个Chicken",
                    "JustNothing现在的班上的某位神秘人2号"
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "我将会因为sin(2x)=2*sin(x)*cos(x)红温很久（忘记*2了，吃我20分）",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "我现在已经不敢动我的代码了，怕再往Xposed模块上面加点东西就炸掉系统了" +
                            "（AI写的代码根本不靠谱，而且很多东西网上查不到）",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "现在距离我开始写小棺材有关的项目已经有",
                    NAME_JUSTNOTHING
            ) {
                @Override
                public String getContent() {
                    Calendar calendar = Calendar.getInstance();
                    long begin = 2023L * 12L + 10L - 1L;
                    long now = calendar.get(Calendar.YEAR) * 12L - calendar.get(Calendar.MONTH);
                    long diff = now - begin;
                    if (diff <= 0) {
                        StringBuilder sb = new StringBuilder("什么鬼，我怎么穿越回我开发工具箱");
                        if (diff < 0) {
                            diff = Math.abs(diff);
                            sb.append("前");
                            if (diff >= 12) sb.append(diff / 12).append("年");
                            if (diff >= 12 && diff % 12L != 0) sb.append("零");
                            if (diff % 12L != 0) sb.append(diff % 12).append("个月");
                            sb.append("了？？？");
                            return sb.toString();
                        } else {
                            sb.append("的那个月了？？？");
                            return sb.toString();
                        }
                    } else {
                        StringBuilder sb = new StringBuilder(content);
                        if (diff >= 12) sb.append(diff / 12).append("年");
                        if (diff >= 12 && diff % 12L != 0) sb.append("零");
                        if (diff % 12L != 0) sb.append(diff % 12).append("个月");
                        sb.append("了");
                        return sb.toString();
                    }
                }
            });


            addDidYouKnowTip(new SimpleTipCallback(
                    """
                            作为一个全程自学，而且历程相当诡异 \
                            (Scratch -> Windows的batch -> Python -> C++ -> Java) 的coder (写代码的人) ，\
                            我的代码风格总是千奇百怪，如果你能上GitHub看到我以前的项目，你就会知道我的码风变过多少次了
                            
                            （其实这还得赖batch，我看的教程全都是码风比较差的，\
                            而且batch面向过程，我直到2024年下半写ClassManager才有面向模块化和对象的意识）
                            
                            （我以前甚至有在Python里面通过exec执行代码，鉴定为脚本式语言受害者）
                            """,
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "你看到的并不一定是事实，就好比现在，这句话也不一定是事实",
                    NAME_JUSTNOTHING
            ));

            return map;
        }

        private static void addDidYouKnowTip(SimpleTipCallback tip) {
            map.put(++count, tip);
        }

        private static void clearDidYouKnowTips() {
            map.clear();
            count = 0;
        }

        private static int getDidTipCount() {
            return count;
        }
    }
}
