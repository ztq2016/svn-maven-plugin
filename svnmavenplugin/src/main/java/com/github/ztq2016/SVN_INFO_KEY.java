package com.github.ztq2016;

public enum SVN_INFO_KEY {
    /**svn对应的url*/
    URL("URL"),
    /**当前仓库的根目录*/
    Repository_Root("Repository Root"),
    /**当前仓库的根目录版本*/
    Revision("Revision"),
    /**当前仓库的最后修改人*/
    Last_Changed_Author("Last Changed Author"),
    /**当前仓库的最后修改版本*/
    Last_Changed_Rev("Last Changed Rev"),
    /**当前仓库最后修改时间*/
    Last_Changed_Date("Last Changed Date");
    
    private String key;
    SVN_INFO_KEY(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
    
}
