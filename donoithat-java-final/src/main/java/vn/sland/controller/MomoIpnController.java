package vn.sland.controller;

import org.springframework.web.bind.annotation.*;import vn.sland.service.MomoService;import java.util.*;

@RestController
public class MomoIpnController {
 private final MomoService momo;public MomoIpnController(MomoService momo){this.momo=momo;}
 @PostMapping({"/momo_ipn.php","/momo_callback.php"})Map<String,Object> ipn(@RequestBody Map<String,Object> payload){if(!momo.verifyCallback(payload))return Map.of("resultCode",99,"message","Invalid signature");return Map.of("resultCode",0,"message","Success");}
}
