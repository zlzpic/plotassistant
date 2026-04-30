package com.bdu.plotassistant.service;

import com.bdu.plotassistant.dto.request.*;
import com.bdu.plotassistant.dto.request.consistencyreport.ResolveConflictRequest;
import com.bdu.plotassistant.dto.request.consistencyreport.TriggerCheckRequest;
import com.bdu.plotassistant.dto.response.*;
import com.bdu.plotassistant.dto.response.consistencyreport.ConsistencyReportDTO;
import com.bdu.plotassistant.dto.response.consistencyreport.ConsistencyReportDetailDTO;
import com.bdu.plotassistant.dto.response.consistencyreport.ReportHistoryDTO;

import java.util.List;

public interface ConsistencyReportService {

    Long triggerCheck(Long projectId, TriggerCheckRequest request);

    ConsistencyReportDTO getLatest(Long projectId);

    List<ReportHistoryDTO> listHistory(Long projectId, Integer limit);

    ConsistencyReportDetailDTO getDetail(Long reportId);

    void markResolved(Long reportId, ResolveConflictRequest request);

    /*void deactivateOldReports(Long projectId);*/
}
