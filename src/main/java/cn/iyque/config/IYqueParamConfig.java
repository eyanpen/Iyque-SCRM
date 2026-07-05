package cn.iyque.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "iyque")
@Data
public class IYqueParamConfig {

    /**
     * 系统审计默认用户名 (updateBy / createBy 字段的兜底值)。
     *
     * 登录凭据自 2026-07 起完全存在 iyque_admin_user 表; 该字段仅作为审计标识兜底,
     * 当无法从 SecurityContext 中获得当前登录用户时使用。
     *
     * 未来若需要真正的"当前登录用户"审计, 应改为从 JWT / SecurityUtils 取值。
     */
    private String userName = "system";

    /**
     * @deprecated 登录密码已迁移至 iyque_admin_user 表, 本字段保留仅为兼容遗留代码, 不再参与鉴权。
     */
    @Deprecated
    private String pwd;

    private Boolean demo=false;

    private String uploadDir;

    //文件预览访问前缀
    private String fileViewUrl;


    //是否预审所有数据,fasle默认预审当天0点到此刻的数据，true所有数据
    private Boolean inquiryAll=false;

    //投诉页面
    private String complaintUrl="https://iyque.cn";


    //客户公海页面
    private String customerSeasUrl="https://iyque.cn";

    //h5营销链接地址
    private String h5MarketUrl="https://iyque.cn";


    //向量数据库相关配置
    private VectorStoreParam vector;



    //三方登录相关参数
    private ThreeLoginParam threeLoginParam=new ThreeLoginParam();





    @Data
    public static class ThreeLoginParam{

        //是否启动三方登录，默认是不启动
        private boolean startThreeLogin=false;


        //gitee登录相关信息
        private GiteeLoginParam giteeLoginParam=new GiteeLoginParam();





    }


    @Data
    public static class GiteeLoginParam{


        //获取授权码code地址
        private String threeLoginUrl="https://gitee.com/oauth/authorize?client_id={0}&redirect_uri={1}&response_type=code";


        //获取token地址
        private String getTokenUrl ="https://gitee.com/oauth/token";


        //判断用户是否star的url
        private String starUlr="https://gitee.com/api/v5/user/starred/iyque/iYqueCode?access_token={0}";


        //star仓库的url
        private String starRepositoryUrl="https://gitee.com/api/v5/user/starred/iyque/iYqueCode";



        //客户端id
        private String clientId;

        //客户端密钥
        private String clientSecret;

        //重定向地址
        private String redirectUri;

    }



    //向量相关设置
    @Data
    public static class VectorStoreParam{

        //片段截取字符数,控制分片的长度，确保均匀分割
        //bge-small-zh-v1.5 embedding 上限约 512 token，中文近似一字一 token，
        //保守取 500 字符，避免单块被 embedding 端截断丢内容。
        private int chunkSize=500;
        //重叠数	保留分片边界的上下文，避免语义断裂,通常设置为片段数的10%-20%之间
        private int chunkOverlap=100;

        //RAG 检索 top-K：Qdrant nearest search 返回的候选片段条数
        //原先硬编码 3 太少，调 6 保证召回覆盖面；可在 conf.yaml 里覆盖。
        private int topK=6;

        //向量维度（需与实际数据匹配）
        private  Integer dimension=1536;

        //自定义集合名称
        private  String collectionName;

        //向量模型
        private String vectorModel;

        //余弦相似度
        private Float score=0.5f;

        // ==================================================================
        // Qdrant 相关配置 (替代原 Milvus host/port)
        // ==================================================================
        // Qdrant HTTP 端点，如
        //   https://xxx.eu-west-1-0.aws.cloud.qdrant.io
        //   http://127.0.0.1:6333
        private String endpoint;

        // Qdrant API Key (Cloud 使用；self-hosted 可留空)
        private String apiKey;

    }
}
