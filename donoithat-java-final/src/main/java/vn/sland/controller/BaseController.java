package vn.sland.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import vn.sland.util.WebUtil;

public abstract class BaseController {
    protected void base(Model m,HttpSession s,String title){m.addAttribute("pageTitle",title);m.addAttribute("loggedIn",WebUtil.logged(s));m.addAttribute("isAdmin",WebUtil.admin(s));m.addAttribute("cartCount",WebUtil.cart(s).stream().mapToInt(x->WebUtil.intv(x.get("quantity"))).sum());m.addAttribute("money",new Object(){public String fmt(Object x){return WebUtil.money(x);}});m.addAttribute("username",s.getAttribute("username"));}
}
