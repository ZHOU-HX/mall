package com.hxzhou.mall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.hxzhou.common.constant.AuthServerConstant;
import com.hxzhou.common.exception.BizCodeEnum;
import com.hxzhou.common.to.MemberRespVo;
import com.hxzhou.common.utils.R;
import com.hxzhou.mall.auth.feign.MemberFeignService;
import com.hxzhou.mall.auth.feign.ThirdPartyFeignService;
import com.hxzhou.mall.auth.vo.UserLoginVo;
import com.hxzhou.mall.auth.vo.UserRegistVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {

    /**
     * 下述被注释掉的，已经在MallWebConfig文件中定义了
    @GetMapping("/login.html")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/reg.html")
    public String regPage() {
        return "reg";
    }
     */

    @Autowired
    ThirdPartyFeignService thirdPartyFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    MemberFeignService memberFeignService;

    /**
     * 发送验证码功能实现
     * @param phone
     * @return
     */
    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone) {
        // 1 接口防刷
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if(!StringUtils.isEmpty(redisCode)) {
            long time = Long.parseLong(redisCode.split("_")[1]);
            if(System.currentTimeMillis() - time < 60000) {
                // 60秒内不可以重复发送获取验证码
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        // 2 验证码的再次校验 redis。存key-phone，value-code。 例如：sms:code:15842071679 -> 123456
        String code = UUID.randomUUID().toString().substring(0, 5) + '_' + System.currentTimeMillis();
        // redis缓存验证码
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, code, 5, TimeUnit.MINUTES);

        thirdPartyFeignService.sendCode(phone, code.split("_")[0]);
        return R.ok();
    }

    /**
     * 注册功能实现
     * RedirectAttributes redirectAttributes：模拟重定向携带数据
     * // TODO：重定向携带数据，利用session原理，将数据放在session中。（只要跳到下一个页面取出这个数据后，session里面的数据就会删除掉）
     * // TODO：分布式下的session问题
     * @return
     */
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo userRegistVo, BindingResult result, RedirectAttributes redirectAttributes) {
        /**
         * 校验出错，转发到注册页
         */
        if(result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream().collect(
                    Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));

            redirectAttributes.addFlashAttribute("errors", errors);

            return "redirect:http://auth.mall.com/reg.html";
        }

        /**
         * 真正注册。调用远程服务进行注册
         */
        // 1 校验验证码
        String code = userRegistVo.getCode();
        String trueCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + userRegistVo.getPhone());

        if(StringUtils.isEmpty(trueCode) || !code.equals(trueCode.split("_")[0])) {
            // 缓存中验证码为空，说明还没发送验证码；或者用户提交的验证码与缓存中的不匹配。返回错误信息给前端
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码信息不对！");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.mall.com/reg.html";
        }

        // 2 验证码校验通过后，删除缓存中的验证码（令牌机制）
        redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + userRegistVo.getPhone());

        // 3 调用远程member注册服务功能
        R r = memberFeignService.regist(userRegistVo);
        if(r.getCode() != 0) {
            // 注册出现错误
            HashMap<String, String> errors = new HashMap<>();
            errors.put("msg", r.getData("msg", new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.mall.com/reg.html";
        }

        // 注册成功回到首页，回到登录页
        return "redirect:http://auth.mall.com/login.html";
    }

    /**
     * 处理登录请求：如果已经登陆，跳回主界面；如果没有登录，才保持在登录页界面
     * @return
     */
    @GetMapping("/login.html")
    public String loginPage(HttpSession session) {
        if(session.getAttribute(AuthServerConstant.LOGIN_USER) != null) {
            return "redirect:http://mall.com";
        }

        return "login";
    }

    /**
     * 登录功能实现
     * @param userLoginVo
     * @return
     */
    @PostMapping("/login")
    public String login(UserLoginVo userLoginVo, RedirectAttributes redirectAttributes, HttpSession session) {

        // 调用远程登陆
        R login = memberFeignService.login(userLoginVo);

        // 登录失败，跳回登录界面
        if(login.getCode() != 0) {
            HashMap<String, String> errors = new HashMap<>();
            errors.put("msg", login.getData("msg", new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.mall.com/login.html";
        }

        // 将用户信息存储在session中返回给前端界面
        MemberRespVo data = login.getData("data", new TypeReference<MemberRespVo>() {});
        session.setAttribute(AuthServerConstant.LOGIN_USER, data);

        // 登录成功，跳向主界面
        return "redirect:http://mall.com";
    }
}
