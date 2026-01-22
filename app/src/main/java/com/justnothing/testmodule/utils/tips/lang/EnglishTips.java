package com.justnothing.testmodule.utils.tips.lang;

import com.justnothing.testmodule.utils.tips.SimpleTipCallback;
import com.justnothing.testmodule.utils.tips.SpecialTipCallback;
import com.justnothing.testmodule.utils.tips.TipSystem;

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

public class EnglishTips {

    private static final Random random = new Random(System.currentTimeMillis());
    
    public static class SpecialTips {
        public static List<SpecialTipCallback> getSpecialTips() {
            List<SpecialTipCallback> specialTips = new ArrayList<>();

            Supplier<String> t = () -> {
                List<String> stringList = List.of(
                    "Kind of charity, isn't it?",
                    "Good luck!",
                    "Getting in bootloader doesn't means the system is corrupted \n(I believe you know this, but some people don't)",
                    "JustNothing's module is just nothing"
                );
                return stringList.get(Math.abs(random.nextInt()) % stringList.size());
            };
            specialTips.add(new SpecialTipCallback(
                    t.get(),
                    0
            ));
            
            specialTips.add(new SpecialTipCallback(
                "Happy New Year!",
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
                "Today is Arbor Day! Even if you don't plant trees, you can protect the environment, like turning off lights when not needed",
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
                "April Fools' Day!",
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
                "Today is Labor Day! Let's not forget the contributions of workers on any day!",
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
                "Today is Youth Day - A festival for us!",
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
                "Today is Mother's Day! Why not send her some blessings?",
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
                "If you're a student, go wish your teachers a Happy Teachers' Day!",
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
                "Merry Christmas!",
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
                "Happy Valentine's Day!",
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
                "Today is Women's Day! Happy Women's Day to all women!",
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
                "Children's Day! Happy Children's Day!",
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
                "Today is Father's Day! Go give him a blessing!",
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
                "Today is the founding day of the Communist Party of China! May the Party lead us to great rejuvenation!",
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
                "Today is Army Day of China! Salute to all soldiers!",
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
                "Today is National Day of China! May our motherland prosper and flourish!",
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
                "It's so late! It's already %d:%d, go to sleep!",
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
            
            return specialTips;
        }
    }
    
    public static class DidYouKnowTips {
        
        private static int count = 0;
        private static final Map<Integer, SimpleTipCallback> map = new HashMap<>();
        
        public static Map<Integer, SimpleTipCallback> getDidYouKnowTips() {
            String NAME_JUSTNOTHING = "JustNothing";
            String NAME_NONECOLDWIND = "NoneColdWind";
            String NAME_NAN = "NAN";
            String NAME_BARINFXXK = "BRAINFXXK";
            String NAME_DYD = "DYD";
            String NAME_YUNMO = "Yunmo";
            clearDidYouKnowTips();

            addDidYouKnowTip(new SimpleTipCallback(
                    "Actually, this feature is not very useful, just for entertainment",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "You can modify the source code of this software to make it yours (this package has no obfuscation, I believe in your technical skills)",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "In Windows, hold Win+R to open the Run dialog, type powershell and run as administrator, " +
                            "then type wininit in the powershell window to make the computer blue screen (though I don't know what it's useful for)",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "VBS has many uses, like msgbox is a good way to harass your friend's computer (just kidding)",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "No, you don't know",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Be careful when combining Xposed module parts and application parts, " +
                            "because XposedApi is CompileOnly, if called in the application part it will boom",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "What you're seeing now is the 7th content in \"Did You Know?\"",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    """
                            Because this thing generates randomly, occasionally the same tip appears two or three times,\
                            but if it appears 4 times, 5 times, or even 6 times... then you can go buy a lottery ticket
                            
                            Note:
                            Chance of 3 consecutive times: %.6f%%
                            Chance of 4 consecutive times: %.6f%%
                            Chance of 5 consecutive times: %.6f%%
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
                    "JustNothing has complex interests, usually writes some music, does embedded development, plays MC, " +
                            "Honor of Kings (quit almost a year ago), Eggy Party (weird right?), Genshin Impact (recently returned), occasionally programming (like now)",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "I just looked at what I wrote, wow, reached the 10th tip, not easy",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Actually, this Xposed module has a matching Magisk module that can add a command line called \"methods\", " +
                            "the code executor in the script actually executes it",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "It turns out, AI is useful (actually the last fantasy before AI forgetting context, later still need to edit myself)",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "In Windows cmd, type color f7 to start Genshin Impact (not recommended at night)",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "NoneColdWind's evaluation of my first Did You Know Tip " +
                            "(\"Actually I don't know what to write, so I just write this\") " +
                            "is: Listening to your words is like listening to your words (?)",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "In the Run dialog opened by Win+R, type regedit and run to open the registry editor, just look, " +
                            "don't be stupid and delete important things (don't ask how I know)",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Actually, just input a command: adb shell pm uninstall " +
                            "--user 0 com.xtc.i3launcher to paralyze the watch's launcher (uninstall the launcher). " +
                            "It can be saved by reinstalling a launcher with higher version than factory, but still not recommended to try",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Although I don't know which time you're seeing this, but if you see it the first time, that's some kind of luck?",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Generally, when using extended commands, type help, --help or .help to see the command usage tutorial",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "NoneColdWind also has complex interests, and his computer configuration is not very good (actually terrible)",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Generally, use AMD for low budget, NVIDIA for sufficient budget",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    """
                            Recommended cost-effective configuration (800 yuan): Intel_E3-1231v3+Jingyue H97M-VH-PLUS\
                            +AMD_RX580-8G+DDR3-1600MHz-8G*2+500WS power supply+four copper tube cooler
                            
                            (At least for someone with \"sufficient budget\" like me)
                            """,
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "NoneColdWind's favorite graphics card - Colorful iGame GeForce RTX4060Ti Ultra W DOC",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "In MC, hold F3+C for the ojng (mojang without ma) easter egg",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "In MC, the Nether is a \"good\" place to sleep",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Tips 19 to 31 are written by NoneColdWind (\\\\\"....\"//)",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "As everyone knows, JRE and JDK optimizations are the best in the programming world (just kidding)",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "If the brain is not responding, please don't try to force close it, because you might die on the spot",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "In MC, loli_pickaxe is a good mod, because in 1.12.2, it can make your friend's computer blue screen (will be blocked by 360)",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Looked at it, Wow, the 29th tip",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Genshin Impact is a good thing, it made my grades rise, my stress resistance improved, but my blood pressure also rose",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "About Intel Pentium E5300+ATI Radeon HD 4350 Series" +
                            "(2009 old device {NoneColdWind's}) can't play Genshin Impact but can play Honkai Impact 3",
                    NAME_NONECOLDWIND
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "JustNothing has a big computer that's almost %d years old, but it's still healthy",
                    NAME_JUSTNOTHING
            ) {
                @Override
                public String getContent() {
                    Calendar calendar = Calendar.getInstance();
                    int year = calendar.get(Calendar.YEAR);
                    if (year <= 2015) return "Did I travel back to " + year + "???";
                    return String.format(Locale.getDefault(), content, year - 2015);
                }
            });

            addDidYouKnowTip(new SimpleTipCallback(
                    "It turns out, if I look at this script one more time I'll spiral into the sky and explode (?)",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Early when I was cracking, I was also smart that " + 
                    "I couldn't connect the watch to the computer for a long time and thought the watch was broken" +
                    "(later found out I didn't install the driver)",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    """
                            Me, NoneColdWind and NAN once researched a very new encrypted text transmission method for cheating in exams\
                            (Just kidding, but the research was real)
                            
                            But unfortunately we never used it before graduation (wrong attention point)
                            """,
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "When I first used WIN11, I always felt a bit uncomfortable, probably because the right-click menu was folded, " +
                    "but I didn't know how to turn this thing off, so I downgraded back",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "After quitting Genshin Impact for more than half a year, during the physical examination, " +
                    "my systolic blood pressure dropped by 4mmHg compared to last semester, but NoneColdWind just returned to Genshin, " +
                    "his systolic blood pressure rose...... What does this prove?",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Now I'm reviving the old \"Did You Know?\", and have to change all the previous content to past tense (explosion)",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "RavenField is a good battlefield simulator, but my brother usually likes to play the vehicles inside, " +
                            "and always uses vehicles to \"charge forward\", so I call it Car Destroyer and Plane Crasher (but my brother seems to like this name?)",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "This is NAN's first compiled content",
                    NAME_NAN
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "All developers who compiled this play \"Genshin Impact\"",
                    NAME_NAN
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "When you reach the 43rd tip, you'll find you've been tricked",
                    NAME_NAN
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Actually there's no 42nd tip, don't believe me go check the source code " + 
                    "\n\n(JustNothing: It's already compiled into apk... Will you still check?)",
                    NAME_NAN
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    """
                        101001110011111 111100101011110 1111111100001100 \
                        101010000101111 101001010101000 1111111100000001
                        
                        (JustNothing friendly translation: Genshin Impact, launch! (In Chinese))"))
                        """,
                    NAME_NAN
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    """
                    Back then, civilizing the physique and barbarizing the spirit was the basic characteristic of Class 16

                    (Tip: "barbarizing the spirit and civilizing the physique" is a saying in Chinese)
                    """,
                    NAME_NAN
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    """
                    Remember during the basketball game, when NAN saw the opposing class's score was more than ten points higher than our class,\
                    the student council regular record sheet in his hand (which could deduct points from the opposing class) got tighter...
                    
                    (Of course, he didn't deduct points of the opposing class)
                    """,
                    NAME_NAN
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    """
                        NoneColdWind deals with 0 and 1 every day, the girls in Class 16 always talk about topics related to 0 and 1
                        
                        (JustNothing: Thinking... Unhandled Exception: java.lang.NullPointerException)
                        (JustNothing: I don't know if this expression is understandable out of China...)
                        """,
                    NAME_NAN
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "NAN doesn't have good equipment\n" +
                        "(JustNothing: It's true, he even uses a 3-year-old electronic chessboard as his main device)",
                    NAME_NAN
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Tips 40 to 48 are written by NAN, except for those written by NoneColdWind and BRAINFXXK, the rest are written by JustNothing",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Doesn't anyone think Java is weird? new returns something that's not a pointer (confusion from a C++ two-year user switching to Java)\n" +
                    "(NoneColdWind: Memory safety is incredible, isn't it?)",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "I don't know how to translate the original content, so I just wrote this (My english isn't good though)",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    """
                        Back when I used Notepad and ancient craftsmanship (kidding) to handcraft Windows batch files, \
                        when I saw \"ECHO is off.\" or \"The syntax of the command is incorrect.\"\
                        I would always get mad for a long time, now don't worry, \
                        we have Undefined Behavior and NullPointerException (dies)
                        
                        (AndroidStudio: Replace with Object.requireNonNull(...))
                        """,
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Daily question, to prevent you from playing computer into getting dementia: 33+33=?" ,
                    NAME_BARINFXXK
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Searching in database... -1 results found" ,
                    NAME_BARINFXXK
            ));

            Supplier<String> s = () -> {
                StringBuilder sb = new StringBuilder("Look there, there's a house! No, look carefully, it's a");
                for (int i = 0; i < 114; i++) sb.append("\n");
                sb.append("HOUSE!!!!");
                return sb.toString();
            };
            addDidYouKnowTip(new SimpleTipCallback(
                    s.get(),
                    NAME_BARINFXXK
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Actually, every time you breathe for 60 seconds, your lifespan decreases by one minute (so not breathing means eternal life?)",
                    NAME_BARINFXXK
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Actually, there really is a website 114514.com (try it if you don't believe me)",
                    NAME_BARINFXXK
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Actually, I've been thinking about how to reproduce the previous special input trigger easter egg",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "Actually, we who write XP modules are also helpless, if we create too many threads or touch the file system in the early stages it will freeze, " +
                            "even calling recycle() on Parcel in Binder will make the system explode ( made me troubleshoot for a long time)",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "JustNothing is fragile, might be instakilled by various people (especially diseases) and things, but NoneColdWind is the opposite",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    """
                    .....././././../.,,,,,////sadkhsafdkhgbkjelkqweleqwkjjqwmneb\
                    khqewnkqewgrjqhgwkurwqekujykqsjh,kqj,m
                    
                    (Written by someone in our former Class 16, maybe a kind of mystery language)
                    """,
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "YEAH!!!!!!! WE WIN!!!!!!!!!!",
                    "Mysterious person #1 in JustNothing's current class"
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "(JustNothing: I don't how to translate this...)",
                    "Mysterious person #1 in JustNothing's current class"
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "You chicken",
                    "Mysterious person #2 in JustNothing's current class"
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "I lost 20 marks in a test because of a simple formula sin(2x)=2*sin(x)*cos(x) (I forgot to multiply 2...)",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "I don't dare to touch my code now, afraid that adding something to the Xposed module will blow up the system" +
                            "(AI-written code is completely unreliable, and many things can't be found online)",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "It has been",
                    NAME_JUSTNOTHING
            ) {
                @Override
                public String getContent() {
                    Calendar calendar = Calendar.getInstance();
                    long begin = 2023L * 12L + 10L - 1L;
                    long now = calendar.get(Calendar.YEAR) * 12L - calendar.get(Calendar.MONTH);
                    long diff = now - begin;
                    if (diff <= 0) {
                        StringBuilder sb = new StringBuilder("What the hell, did I traveled back to the month");
                        if (diff < 0) {
                            diff = Math.abs(diff);
                            sb.append(" before my first project started");
                            if (diff >= 12) sb.append(diff / 12).append(" years ");
                            if (diff >= 12 && diff % 12L != 0) sb.append(" and ");
                            if (diff % 12L != 0) sb.append(diff % 12).append(" months");
                            sb.append("???");
                            return sb.toString();
                        } else {
                            sb.append(" that my first project started ???");
                            return sb.toString();
                        }
                    } else {
                        StringBuilder sb = new StringBuilder(content);
                        if (diff >= 12) sb.append(diff / 12).append(" years ");
                        if (diff >= 12 && diff % 12L != 0) sb.append("and ");
                        if (diff % 12L != 0) sb.append(diff % 12).append(" months ");
                        sb.append("since I started writing projects related to XTC");
                        return sb.toString();
                    }
                }
            });

            addDidYouKnowTip(new SimpleTipCallback(
                    """
                        As someone who is completely self-taught, and with a rather strange learning path \
                        (Scratch -> Windows batch -> Python -> C++ -> Java) coder,\
                        my coding style has always been weird, if you can go to GitHub and see my previous projects, \
                        you'll know how many times my coding style has changed
                        
                        (Actually, this is still because of batch, the tutorials I watched all had poor coding style,\
                        and batch is procedural, I didn't have the awareness of modularization and object orientation \
                        until I wrote ClassManager in the second half of 2024)
                        
                        (I even used to execute code through exec in Python, identified as a victim of scripting languages)
                        """,
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "What you see is not necessarily the truth, just like now, this sentence is not necessarily the truth",
                    NAME_JUSTNOTHING
            ));

            addDidYouKnowTip(new SimpleTipCallback(
                    "In fact, I made a lot of grammar mistakes in translation, but so do the AI " + 
                    "(I used AI to translate all tips and edited some of them, seems like AI is also not good in grammar)",
                    NAME_JUSTNOTHING
            ));

            return map;
        }
        
        private static void addDidYouKnowTip(SimpleTipCallback tip) {
            count++;
            map.put(count, tip);
        }
        
        private static void clearDidYouKnowTips() {
            count = 0;
            map.clear();
        }
        
        private static int getDidTipCount() {
            return count;
        }
        
        public static void addTips(TipSystem tipSystem) {
            Map<Integer, SimpleTipCallback> tips = getDidYouKnowTips();
            for (SimpleTipCallback tip : tips.values()) {
                tipSystem.addDidYouKnowTip(tip);
            }
        }
    }
}
