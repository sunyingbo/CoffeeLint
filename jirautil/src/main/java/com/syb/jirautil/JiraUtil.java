package com.syb.jirautil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.JiraRestClientFactory;
import com.atlassian.jira.rest.client.domain.BasicIssue;
import com.atlassian.jira.rest.client.domain.Comment;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.domain.IssueType;
import com.atlassian.jira.rest.client.domain.SearchResult;
import com.atlassian.jira.rest.client.domain.User;
import com.atlassian.jira.rest.client.domain.input.FieldInput;
import com.atlassian.jira.rest.client.domain.input.IssueInput;
import com.atlassian.jira.rest.client.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.domain.input.TransitionInput;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

/**
 * jira java工具类
 * jira-rest-java-client-2.0.0-m2.jar
 */
public class JiraUtil {

    private static JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
    private static String uri = "";
    private static String user = "";
    private static String pwd = "";
    private static JiraRestClient restClient;

    /**
     * 获得jira的客户端
     *
     * @return JiraRestClient
     * @throws URISyntaxException
     */
    private static JiraRestClient getJiraRestClient() throws URISyntaxException {
        if (restClient == null) {
            URI jiraServerUri = new URI(uri);
            restClient = factory.createWithBasicHttpAuthentication(
                    jiraServerUri, user, pwd);
        }
        return restClient;
    }

    /**
     * 获得工单信息
     *
     * @param issueKey
     *            工单key，比如：NQCP-5
     * @throws URISyntaxException
     */
    public static Issue getIssue(String issueKey) throws URISyntaxException {
        JiraRestClient restClient = getJiraRestClient();
        return restClient.getIssueClient().getIssue(issueKey).claim();
    }


    /**
     * 检索工单
     * @param jql
     *
     * @return
     * @throws URISyntaxException
     */
    public static Iterable<BasicIssue> searchIssues(String jql) throws URISyntaxException{
        JiraRestClient restClient = getJiraRestClient();
        SearchResult searchResutl = restClient.getSearchClient().searchJql(jql).claim();
        return searchResutl.getIssues();
    }

    /**
     * 检索工单
     * @param jql
     * @param startIndex
     * @param maxResults
     * @return
     * @throws URISyntaxException
     */
    public static Iterable<BasicIssue> searchIssues(String jql,int startIndex, int maxResults) throws URISyntaxException{
        JiraRestClient restClient = getJiraRestClient();
        SearchResult searchResutl = restClient.getSearchClient().searchJql(jql,maxResults,startIndex).claim();
        return searchResutl.getIssues();
    }


    /**
     * 打印jira系统中已经创建的全部issueType
     * issuetype/
     *
     * @throws URISyntaxException
     */
    public static Iterable<IssueType> printAllIssueType() throws URISyntaxException {
        JiraRestClient restClient = getJiraRestClient();
        return restClient.getMetadataClient().getIssueTypes().claim();

    }

    /**
     * 创建一个新工单
     *
     * @param projectKey
     *            项目key，比如：NQCP
     * @param issueType
     *            工单类型，来源于printAllIssueType()的id
     * @param description
     *            工单描述
     * @param summary
     *            工单主题
     * @param assignee
     *            工单负责人
     * @throws URISyntaxException
     */
    public static BasicIssue createIssue(String projectKey, Long issueType,
                                         String description, String summary, String assignee)
            throws URISyntaxException {
        JiraRestClient restClient = getJiraRestClient();
        IssueInputBuilder issueBuilder = new IssueInputBuilder(projectKey, issueType);
        issueBuilder.setDescription(description);
        issueBuilder.setSummary(summary);
        if (getUser(assignee) != null) {
            issueBuilder.setAssigneeName(assignee);
        }
        IssueInput issueInput = issueBuilder.build();
        return restClient.getIssueClient().createIssue(issueInput).claim();
    }

    /**
     * 获取用户信息
     *
     * @param username
     * @return
     * @throws URISyntaxException
     */
    public static User getUser(String username) throws URISyntaxException {
        JiraRestClient restClient = getJiraRestClient();
        return restClient.getUserClient().getUser(username).claim();
    }

    /**
     * 添加备注
     * @param issue 需要添加备注的问题单
     * @param comment 备注内容
     * @throws URISyntaxException
     */
    public static void addComment(Issue issue, String comment) throws URISyntaxException {
        JiraRestClient restClient = getJiraRestClient();
        restClient.getIssueClient().addComment(issue.getCommentsUri(), Comment.valueOf(comment)).claim();
    }

    /**
     * 改变工单workflow状态 issue的workflow是不可以随便改变的，必须按照流程图的顺序进行改变，具体如下：
     *
     * 当前状态 ：说明                      变更流程id:说明 >> 变更后状态
     1:open，开放                          1)4:start progress >> in progerss 2)5:resolve issue >> resolved 3)2:close issue >> closed
     3:in progerss 正在处理                1)301:stop progress >> open 2)5:resolve issue >> resolved 3)2:close issue >> closed
     4:reopened 重新打开                     1)701:close issue >> closed 2)3:reopen issue >> reopened
     5:resolved 已解决                   1)4:start progress >> in progerss 2)5:resolve issue >> resolved 3)2:close issue >> closed
     6:closed 已关闭                       1)3:reopen issue >> reopened
     *
     *
     * 可通过如下方式查看当前工单的后续工作流程： Iterable<Transition> iter =
     * restClient.getIssueClient().getTransitions(issue).claim();
     *
     * for (Transition transition : iter) { System.out.println(transition); }
     *
     * 输出结果：当前工单状态是 5:reopened 重新打开 Transition{id=4, name=Start Progress,
     * fields=[]} Transition{id=5, name=Resolve Issue,
     * fields=[Field{id=fixVersions, isRequired=false, type=array},
     * Field{id=resolution, isRequired=true, type=resolution}]} Transition{id=2,
     * name=Close Issue, fields=[Field{id=fixVersions, isRequired=false,
     * type=array}, Field{id=resolution, isRequired=true, type=resolution}]}
     *
     *
     * @param issuekey
     *            工单key
     * @param statusId
     *            变更流程id
     * @param fields
     *            随状态需要传递的参数，可以为空
     * @throws URISyntaxException
     */
    public static void changeIssueStatus(String issuekey, int statusId, Collection<FieldInput> fields)
            throws URISyntaxException {
        JiraRestClient restClient = getJiraRestClient();
        Issue issue = getIssue(issuekey);
        if (issue != null) {
            TransitionInput tinput = new TransitionInput(statusId, fields);
            restClient.getIssueClient().transition(issue, tinput);
        }
    }

}
