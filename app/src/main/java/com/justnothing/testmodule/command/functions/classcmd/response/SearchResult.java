package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class SearchResult extends ClassCommandResult {

    private String searchType;  // "class", "method", "field", "annotation"
    private String pattern;
    private List<String> matchedClasses;
    private List<MatchedMethod> matchedMethods;
    private List<FieldInfo> matchedFields;
    private List<MatchedAnnotation> matchedAnnotations;
    private int totalCount;

    public SearchResult() {
        super();
        this.matchedClasses = new ArrayList<>();
        this.matchedMethods = new ArrayList<>();
        this.matchedFields = new ArrayList<>();
        this.matchedAnnotations = new ArrayList<>();
    }

    public SearchResult(String requestId) {
        super(requestId);
        this.matchedClasses = new ArrayList<>();
        this.matchedMethods = new ArrayList<>();
        this.matchedFields = new ArrayList<>();
        this.matchedAnnotations = new ArrayList<>();
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

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("searchType", searchType);
        obj.put("pattern", pattern);
        obj.put("totalCount", totalCount);

        if (!matchedClasses.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (String cls : matchedClasses) { arr.put(cls); }
            obj.put("matchedClasses", arr);
        }

        if (!matchedMethods.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (MatchedMethod m : matchedMethods) { arr.put(m.toJson()); }
            obj.put("matchedMethods", arr);
        }

        if (!matchedFields.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (FieldInfo f : matchedFields) { arr.put(f.toJson()); }
            obj.put("matchedFields", arr);
        }

        if (!matchedAnnotations.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (MatchedAnnotation a : matchedAnnotations) { arr.put(a.toJson()); }
            obj.put("matchedAnnotations", arr);
        }

        return obj;
    }

    public static class MatchedMethod {
        public String declaringClass;
        public MethodInfo method;

        public MatchedMethod(String declaringClass, MethodInfo method) {
            this.declaringClass = declaringClass;
            this.method = method;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("declaringClass", declaringClass);
            obj.put("method", method.toJson());
            return obj;
        }
    }

    public static class MatchedAnnotation {
        public String declaringClass;
        public String memberName;
        public String annotationName;

        public MatchedAnnotation(String declaringClass, String memberName, String annotationName) {
            this.declaringClass = declaringClass;
            this.memberName = memberName;
            this.annotationName = annotationName;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("declaringClass", declaringClass);
            obj.put("memberName", memberName);
            obj.put("annotationName", annotationName);
            return obj;
        }
    }
}
