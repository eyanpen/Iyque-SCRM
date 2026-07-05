package cn.iyque.controller;


import cn.iyque.annotation.RateLimit;
import cn.iyque.config.IYqueParamConfig;
import cn.iyque.constant.HttpStatus;
import cn.iyque.dao.IYqueAdminUserDao;
import cn.iyque.domain.BaseUserInfo;
import cn.iyque.domain.IYQueAuthInfo;
import cn.iyque.domain.JwtResponse;
import cn.iyque.domain.ResponseResult;
import cn.iyque.entity.IYqueAdminUser;
import cn.iyque.entity.IYqueConfig;
import cn.iyque.service.IYqueConfigService;
import cn.iyque.utils.JwtUtils;
import cn.iyque.utils.TicketUtils;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.result.WxMediaUploadResult;
import me.chanjar.weixin.cp.api.WxCpMediaService;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.WxCpOauth2UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Optional;


/**
 * 系统登录地址
 */
@RestController
@RequestMapping("/iYqueSys")
@Slf4j
public class IYqueLoginController {



    @Autowired
    private IYqueParamConfig iYqueParamConfig;

    @Autowired
    private IYqueConfigService iYqueConfigService;

    @Autowired
    private IYqueAdminUserDao iYqueAdminUserDao;


    /**
     * 简单的登录
     *
     * 所有管理员账号存储在 iyque_admin_user 表. conf.yaml 里不再保存 admin 凭据.
     * seed 数据 (含默认 admin) 由 scripts/installation/mysql-migrations/001-admin-users.sql 导入.
     *
     * @return JWT token
     */
    @PostMapping("/login")
    @RateLimit(attempts = 5, lockTime = 300) // 5次失败后锁定5分钟
    public ResponseResult<JwtResponse> login(@RequestBody IYQueAuthInfo iQyqueAuthInfo){

        String reqUser = iQyqueAuthInfo.getUsername();
        String reqPwd  = iQyqueAuthInfo.getPassword();

        try {
            Optional<IYqueAdminUser> found = iYqueAdminUserDao.findByUsernameAndDelFlag(reqUser, 0);
            if (found.isPresent() && found.get().getPassword().equals(reqPwd)) {
                log.info("管理员登录 [{}] 命中 iyque_admin_user (id={})",
                        reqUser, found.get().getId());
                return new ResponseResult<>(JwtResponse.builder()
                        .token(JwtUtils.generateToken(found.get().getUsername()))
                        .build());
            }
        } catch (Exception e) {
            log.warn("查询 iyque_admin_user 失败 (可能表尚未创建): {}", e.getMessage());
        }

        log.info("管理员登录 [{}] 账号或密码错误", reqUser);
        return new ResponseResult<>(HttpStatus.ERROR, "账号或密码错误", null);
    }


    /**
     * 企微h5登录
     * @param authCode
     * @return
     */
    @GetMapping("/weComLogin")
    public  ResponseResult<JwtResponse> weComLogin(String authCode) {

        try {
            WxCpService wxcpservice = iYqueConfigService.findWxcpservice();

            WxCpOauth2UserInfo userInfo = wxcpservice.getOauth2Service().getUserInfo(authCode);
            if(null != userInfo && StringUtils.isNotEmpty(userInfo.getUserId())){
                return new ResponseResult<>(JwtResponse.builder()
                        .token(JwtUtils.generateToken(userInfo.getUserId()))
                        .build());
            }
        }catch (Exception e){
            log.error("企微h5登录失败:"+e.getMessage());
            return new ResponseResult<>(HttpStatus.ERROR,"企微h5登录失败",null);
        }


        return new ResponseResult<>(HttpStatus.ERROR,"账号登录失败",null);

    }



    /**
     * 企微h5回掉地址
     * @param redirectUrl
     * @return
     */
    @GetMapping("/weComRedirect")
    public ResponseResult weComRedirect(String redirectUrl){

        StringBuilder sb=new StringBuilder();

        try {
            WxCpService wxcpservice = iYqueConfigService.findWxcpservice();

            sb.append(
                    wxcpservice.getOauth2Service()
                            .buildAuthorizationUrl(redirectUrl,"iYque","snsapi_privateinfo")
            );

        }catch (Exception e){
            log.error("企微h5登录失败:"+e.getMessage());
            return new ResponseResult<>(HttpStatus.ERROR,"企微h5登录失败",null);
        }

        return new ResponseResult(
                sb.toString()
        );
    }


    /**
     * 获取应用的jsapi_ticket
     *
     * @param url JS接口页面的完整URL
     * @return
     */
    @GetMapping("/getAgentTicket")
    public ResponseResult getAgentTicket(String url) {

        try {
            String ticket = iYqueConfigService.findWxcpservice().getAgentJsapiTicket();
            return new ResponseResult( TicketUtils.getSignature(ticket,url));
        }catch (Exception e){
            log.error("获取应用的jsapi_ticket失败"+e.getMessage());
            return new ResponseResult(HttpStatus.ERROR,"获取应用的jsapi_ticket失败",null);
        }

    }


    /**
     * 发送获取素材media_id
     * @param url
     * @param type
     * @param name
     * @return
     */
    @GetMapping("/uploadMediaId")
    public ResponseResult uploadMediaId(String url, String type, String name){

        try {

            WxMediaUploadResult uploadResult = iYqueConfigService.findWxcpservice().getMediaService()
                    .upload(type, name + "." + url.substring(url.lastIndexOf(".") + 1, url.length()), url);

            return new ResponseResult(uploadResult);
        }catch (Exception e){
             log.error("获取临时素材失败media_id"+e.getMessage());
            return new ResponseResult(HttpStatus.ERROR,"获取临时素材失败media_id",null);
        }

    }



    /**
     * 获取基础信息
     *
     * @return 获取基础信息
     */
    @GetMapping("/getBaseInfo")
    public ResponseResult getBaseInfo() {
        IYqueConfig iYqueConfig = iYqueConfigService.findIYqueConfig();

        return new ResponseResult(
                BaseUserInfo.builder()
                        .agentId(iYqueConfig.getAgentId())
                        .corpId(iYqueConfig.getCorpId())
                        .build()
        );

    }














}
