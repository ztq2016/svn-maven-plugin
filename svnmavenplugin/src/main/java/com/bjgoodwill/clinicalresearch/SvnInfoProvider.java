package com.bjgoodwill.clinicalresearch;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

@Mojo(name="info",defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class SvnInfoProvider extends AbstractMojo {
    
    @Parameter(defaultValue = "${project}",readonly = true)
    private MavenProject project;

    @Parameter(property = "maven.svn.lastCommit",defaultValue = "lastCommit")
    private String lastCommit;
    
    @Parameter(property = "maven.svn.branchName",defaultValue = "branchName")
    private String branchName;
    
    @Parameter(property = "maven.svn.lastCommitTime",defaultValue = "lastCommitTime")
    private String lastCommitTime;

    @Parameter(property = "maven.svn.lastCommitAuthor", defaultValue = "lastCommitAuthor")
    private String lastCommitAuthor;
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        String absolutePath = project.getBasedir().getAbsolutePath();
        Map<String,String> map = getSvnInfoMap(absolutePath);
        project.getProperties().setProperty(lastCommit, map.get(SVN_INFO_KEY.Last_Changed_Rev.getKey()));
        String url = map.get(SVN_INFO_KEY.URL.getKey());
        String rootUrl = map.get(SVN_INFO_KEY.Repository_Root.getKey());
        String branch = url.replace(rootUrl, "");
        project.getProperties().setProperty(this.branchName, branch);
        project.getProperties().setProperty(this.lastCommitTime,  map.get(SVN_INFO_KEY.Last_Changed_Date.getKey()));
        project.getProperties().setProperty(this.lastCommitAuthor,map.get(SVN_INFO_KEY.Last_Changed_Author.getKey()));
        getLog().info("自定义插件运行完毕.");
    }

    private Map<String,String> getSvnInfoMap(String absolutePath) {
        Map<String,String> map = null;
        if (!isSvnWorkCopy(absolutePath))  {
            getLog().debug("当前目录不是一个svn仓库.");
        }
        File file = new File(absolutePath);
        String parentUrl = file.getParent();
        if (!isSvnWorkCopy(parentUrl)) {
            getLog().debug("当前目录的父目录不是一个svn仓库.");
            return map;
        } else {
            Runtime runtime = Runtime.getRuntime();
            String svnInfoCommand = "svn info";
            getLog().debug("svn信息的获取位置:" + parentUrl);
            try {
                Process exec = runtime.exec(svnInfoCommand,null, new File(parentUrl));
                int status = exec.waitFor();
                if (status != 0) {
                    InputStream errorStream = exec.getErrorStream();
                    Map<String, String> svnInfoMap = getSvnInfoFromInputStream(errorStream);
                    getLog().debug("svn信息获取失败:");
                }
                int i = exec.exitValue();
                if (i == 0) {
                    //获取svn内容
                    InputStream inputStream = exec.getInputStream();
                    return getSvnInfoFromInputStream(inputStream);
                } else {
                    //获取svn异常信息
                    InputStream errorStream = exec.getErrorStream();
                    String errInfo = getErrInfo(errorStream);
                    getLog().error("svn命令运行错误:" + errInfo);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return map;
        }
    }

    private Map<String, String> getSvnInfoFromInputStream(InputStream inputStream) throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        if (inputStream != null) {
            inputStreamReader = new InputStreamReader(inputStream,getCharset());
            bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                int i = line.indexOf(":");
                if (i == -1) {
                    getLog().info("没有办法处理的svn信息:" + line);
                    continue;
                }
                String key = line.substring(0, i);
                String value = line.substring(i + 1, line.length());
                map.put(key,value);
            }
        }
        close(bufferedReader);
        close(inputStreamReader);
        close(inputStream);
        return map;
    }

    private String getCharset() {
        String osName = System.getProperty("os.name").toLowerCase();
        getLog().debug("当前操作系统类型:"  + osName);
        String charset = "utf-8";
        if (osName.indexOf("windows") >= 0) {
            charset = "GBK";
        }
        return charset;
    }

    private void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String getErrInfo(InputStream errInputStream) throws IOException {
        StringBuffer stringBuffer = new StringBuffer();
        BufferedReader bufferedReader = null;
        InputStreamReader inputStreamReader = null;
        if (errInputStream != null) {
             inputStreamReader = new InputStreamReader(errInputStream,getCharset());
             bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                stringBuffer.append(line);
                stringBuffer.append(System.lineSeparator());
            }
        }
        close(bufferedReader);
        close(inputStreamReader);
        close(errInputStream);
        return stringBuffer.toString();
    }
    /**
     * 判定当前目录是否是svn副本
     * @param absolutePath 目录的绝对路径
     * @return 是副本返回true，不是副本返回false
     */
    private boolean isSvnWorkCopy(String absolutePath) {
        File file = new File(absolutePath);
        File[] files = file.listFiles();
        for (File subFile : files) {
            if (subFile.getName().equals(".svn")) {
                return true;
            }
        }
        return false;
    }
}
