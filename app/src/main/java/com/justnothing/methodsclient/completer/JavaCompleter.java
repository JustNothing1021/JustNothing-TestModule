package com.justnothing.methodsclient.completer;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.StringsCompleter;

import java.util.List;

public class JavaCompleter extends StringsCompleter {

    private static final long COOLDOWN_MS = 300;
    private static long lastCompleteTime = 0;

    public JavaCompleter() {
        super(
                "Appendable", "AutoCloseable", "Boolean", "Byte", "CharSequence",
                "Character", "Class", "ClassLoader", "Cloneable", "Comparable",
                "Compiler", "Double", "Enum", "Error", "Exception", "Float",
                "IllegalArgumentException", "IllegalStateException",
                "IndexOutOfBoundsException", "Integer", "Iterable", "Long",
                "Math", "Number", "Object", "Override", "Package", "Process",
                "Runnable", "Runtime", "Short", "StackTraceElement", "StrictMath",
                "String", "StringBuffer", "StringBuilder", "SuppressWarnings",
                "System", "Thread", "ThreadLocal", "Throwable", "UnsupportedOperationException",
                "Void",

                "AbstractCollection", "AbstractList", "AbstractMap", "AbstractQueue",
                "AbstractSequentialList", "AbstractSet", "ArrayDeque", "ArrayList",
                "Arrays", "Base64", "Calendar", "Collection", "Collections",
                "Comparator", "Currency", "Date", "Deque", "Dictionary",
                "EnumMap", "EnumSet", "Formatter", "GregorianCalendar", "HashMap",
                "HashSet", "Hashtable", "IdentityHashMap", "Iterator", "LinkedHashMap",
                "LinkedHashSet", "LinkedList", "List", "ListIterator", "Locale",
                "Map", "NavigableMap", "NavigableSet", "Objects", "Optional",
                "PriorityQueue", "Properties", "Queue", "Random", "Scanner",
                "Set", "SortedMap", "SortedSet", "Stack", "StringTokenizer",
                "Timer", "TimerTask", "TreeMap", "TreeSet", "UUID", "Vector"
        );
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        long now = System.currentTimeMillis();
        if (now - lastCompleteTime < COOLDOWN_MS) {
            return;
        }
        
        lastCompleteTime = now;
        super.complete(reader, line, candidates);
    }
}
