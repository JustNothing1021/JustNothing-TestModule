package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("Search")
public class SearchResult extends ClassCommandResult {

    private String searchType;

    private String pattern;

    private List<String> matchedClasses = new ArrayList<>();

    private List<MatchedMethod> matchedMethods = new ArrayList<>();

    private List<FieldInfo> matchedFields = new ArrayList<>();

    private List<MatchedAnnotation> matchedAnnotations = new ArrayList<>();

    private int totalCount;

    public SearchResult() {
        super();
    }

    public SearchResult(String requestId) {
        super(requestId);
    }

    public String getSearchType() { return searchType; }
    public void setSearchType(String searchType) { this.searchType = searchType; }
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    public List<String> getMatchedClasses() { return matchedClasses; }
    public void setMatchedClasses(List<String> matchedClasses) { this.matchedClasses = matchedClasses; }
    public List<MatchedMethod> getMatchedMethods() { return matchedMethods; }
    public void setMatchedMethods(List<MatchedMethod> matchedMethods) { this.matchedMethods = matchedMethods; }
    public List<FieldInfo> getMatchedFields() { return matchedFields; }
    public void setMatchedFields(List<FieldInfo> matchedFields) { this.matchedFields = matchedFields; }
    public List<MatchedAnnotation> getMatchedAnnotations() { return matchedAnnotations; }
    public void setMatchedAnnotations(List<MatchedAnnotation> matchedAnnotations) { this.matchedAnnotations = matchedAnnotations; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public static class MatchedMethod {
        public String declaringClass;
        public MethodInfo method;

        public MatchedMethod() {}

        public MatchedMethod(String declaringClass, MethodInfo method) {
            this.declaringClass = declaringClass;
            this.method = method;
        }
    }

    public static class MatchedAnnotation {
        public String declaringClass;
        public String memberName;
        public String annotationName;

        public MatchedAnnotation() {}

        public MatchedAnnotation(String declaringClass, String memberName, String annotationName) {
            this.declaringClass = declaringClass;
            this.memberName = memberName;
            this.annotationName = annotationName;
        }
    }
}
