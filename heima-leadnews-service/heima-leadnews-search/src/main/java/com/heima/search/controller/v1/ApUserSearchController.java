package com.heima.search.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.HistorySearchDto;
import com.heima.search.service.ApUserSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/history")
public class ApUserSearchController {
    @Autowired
    private ApUserSearchService apUserSearchService;

    @PostMapping("/load")
    public ResponseResult findUserSearch() {
        return apUserSearchService.findUserSearch();
    }

    @PostMapping("/del")
    public ResponseResult delUserSearch(@RequestBody HistorySearchDto historySearchDto) {
        return apUserSearchService.delUserSearch(historySearchDto);
    }


}
