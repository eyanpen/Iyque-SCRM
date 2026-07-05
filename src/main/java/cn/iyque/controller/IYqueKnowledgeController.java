package cn.iyque.controller;


import cn.hutool.core.collection.CollectionUtil;
import cn.iyque.chain.vectorstore.IYqueVectorStore;
import cn.iyque.dao.IYqueKnowledgeAttachDao;
import cn.iyque.domain.KnowledgeInfoUploadRequest;
import cn.iyque.domain.ResponseResult;
import cn.iyque.entity.IYqueKnowledgeAttach;
import cn.iyque.entity.IYqueKnowledgeFragment;
import cn.iyque.entity.IYqueKnowledgeInfo;
import cn.iyque.service.*;
import cn.iyque.utils.TableSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * 知识库
 */
@RequestMapping("/knowledge")
@Slf4j
@RestController
public class IYqueKnowledgeController {

    @Autowired
    private IYqueKnowledgeInfoService iYqueKnowledgeInfoService;

    @Autowired
    private IYqueKnowledgeAttachService yqueKnowledgeAttachService;


    @Autowired
    private IYqueKnowledgeFragmentService yqueKnowledgeFragmentService;

    @Autowired
    private IYqueKnowledgeAttachDao yqueKnowledgeAttachDao;

    /**
     * 上传目录, 与 storeContent 保存路径对齐。缺省 upload/ 相对于 backend 工作目录 (Iyque-SCRM/).
     * 想改成绝对路径就在 conf.yaml 里加 iyque.upload.dir=/data/scrm/upload
     */
    @Value("${iyque.upload.dir:upload}")
    private String uploadDir;

    /**
     * 下载原始附件（供 RAG citation 弹窗的"下载原始文件"按钮使用）。
     *
     * 文件查找策略（按顺序尝试，命中即返回）：
     *   1) uploadDir/<docId>_<docName>   （新上传流程未来会用这个前缀）
     *   2) uploadDir/<docName>           （当前 demo PDFs 直接叫原名放在这里）
     * 都不存在返回 404。
     */
    @GetMapping("/attach/download/{docId}")
    public ResponseEntity<Resource> downloadAttach(@PathVariable Long docId) {
        Optional<IYqueKnowledgeAttach> opt = yqueKnowledgeAttachDao.findById(docId);
        if (!opt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        IYqueKnowledgeAttach attach = opt.get();
        String docName = attach.getDocName();
        if (docName == null || docName.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path candidate1 = base.resolve(docId + "_" + docName).normalize();
        Path candidate2 = base.resolve(docName).normalize();

        File found = null;
        // 防目录穿越: 解析后的路径必须仍以 base 开头
        if (candidate1.startsWith(base) && candidate1.toFile().isFile()) {
            found = candidate1.toFile();
        } else if (candidate2.startsWith(base) && candidate2.toFile().isFile()) {
            found = candidate2.toFile();
        }
        if (found == null) {
            log.warn("attach 下载失败: docId={} docName={} 在 {} 下找不到文件", docId, docName, base);
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(found);
        // Content-Disposition 里的中文文件名需要 URL 编码 (RFC 5987)
        String encoded;
        try {
            encoded = URLEncoder.encode(docName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (Exception e) {
            encoded = "file";
        }
        MediaType contentType = MediaTypeFactory.getMediaType(docName)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
                .contentLength(found.length())
                .body(resource);
    }









    /**
     * 获取知识库列表
     * @param knowledgeInfo
     * @return
     */
    @GetMapping("/findKnowledgeByPage")
    public ResponseResult<IYqueKnowledgeInfo> findKnowledgeByPage(IYqueKnowledgeInfo knowledgeInfo){

        Page<IYqueKnowledgeInfo> iYqueKnowledgeInfos = iYqueKnowledgeInfoService.findAll(knowledgeInfo,
                PageRequest.of( TableSupport.buildPageRequest().getPageNum(),
                        TableSupport.buildPageRequest().getPageSize(), Sort.by("createTime").descending()));
        return new ResponseResult(iYqueKnowledgeInfos.getContent(),iYqueKnowledgeInfos.getTotalElements());
    }


    /**
     * 获取所有知识库
     * @return
     */
    @GetMapping("/findAll")
    public ResponseResult<List<IYqueKnowledgeInfo>> findAll(){
       List<IYqueKnowledgeInfo> infoList=iYqueKnowledgeInfoService.findAll();

       return new ResponseResult<>(infoList);

    }



    /**
     * 新增知识库
     */
    @PostMapping("/save")
    public ResponseResult save(@RequestBody IYqueKnowledgeInfo knowledgeInfo) {
        knowledgeInfo.setCreateTime(new Date());
        iYqueKnowledgeInfoService.saveOrUpdate(knowledgeInfo);
        return new ResponseResult();
    }

    /**
     * 上传知识库附件
     */
    @PostMapping(value = "/attach/upload")
    public ResponseResult upload(KnowledgeInfoUploadRequest request){
        iYqueKnowledgeInfoService.upload(request);
        return new ResponseResult();
    }


    /**
     * 删除知识库
     */
    @DeleteMapping("/remove/{id}")
    public ResponseResult remove(@PathVariable Long id){
        iYqueKnowledgeInfoService.removeKnowledge(id);
        return new ResponseResult();
    }


    /**
     * 查询知识附件信息列表
     */
    @GetMapping("/detail/{kid}")
    public  ResponseResult<IYqueKnowledgeAttach> attach(@PathVariable Long kid){
        Page<IYqueKnowledgeAttach> iYqueKnowledgeInfos = yqueKnowledgeAttachService.findAll(IYqueKnowledgeAttach.builder()
                        .kid(kid)
                        .build(),
                PageRequest.of( TableSupport.buildPageRequest().getPageNum(),
                        TableSupport.buildPageRequest().getPageSize(), Sort.by("createTime").descending()));
        return new ResponseResult(iYqueKnowledgeInfos.getContent(),iYqueKnowledgeInfos.getTotalElements());
    }




    /**
     * 删除知识库附件
     *
     */
    @DeleteMapping("/attach/remove/{docId}")
    public ResponseResult removeAttach(@PathVariable Long docId) {
        yqueKnowledgeAttachService.removeKnowledgeAttach(docId);
        return new ResponseResult();
    }


    /**
     * 查询知识片段
     */
    @GetMapping("/fragment/list/{docId}")
    public ResponseResult<IYqueKnowledgeFragment> fragmentList(@PathVariable Long docId) {
        Page<IYqueKnowledgeFragment> iYqueKnowledgeFragments = yqueKnowledgeFragmentService.findAll(IYqueKnowledgeFragment.builder()
                        .docId(docId)
                        .build(),
                PageRequest.of( TableSupport.buildPageRequest().getPageNum(),
                        TableSupport.buildPageRequest().getPageSize(), Sort.by("createTime").descending()));
        return new ResponseResult(iYqueKnowledgeFragments.getContent(),iYqueKnowledgeFragments.getTotalElements());
    }




}
