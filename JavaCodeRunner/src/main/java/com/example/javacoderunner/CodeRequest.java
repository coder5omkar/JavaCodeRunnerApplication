package com.example.javacoderunner;

import java.util.List;

class CodeRequest {
    private String code;
    private List<String> vars;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<String> getVars() {
        return vars;
    }

    public void setVars(List<String> vars) {
        this.vars = vars;
    }

}
