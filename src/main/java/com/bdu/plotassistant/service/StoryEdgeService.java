package com.bdu.plotassistant.service;

import com.bdu.plotassistant.dto.request.storyedge.BatchSaveEdgesRequest;
import com.bdu.plotassistant.dto.request.storyedge.CreateEdgeRequest;
import com.bdu.plotassistant.dto.request.storyedge.GenerateEdgeRequest;
import com.bdu.plotassistant.dto.request.storyedge.UpdateEdgeRequest;
import com.bdu.plotassistant.dto.response.storyedge.EdgeDTO;
import com.bdu.plotassistant.dto.response.storyedge.EdgeDetailDTO;
import com.bdu.plotassistant.dto.response.storyedge.EdgeSuggestionDTO;

import java.util.List;

public interface StoryEdgeService {

    Long create(Long projectId, CreateEdgeRequest request);

    List<EdgeDTO> listByProject(Long projectId);

    EdgeDetailDTO getDetail(Long edgeId);

    void update(Long edgeId, UpdateEdgeRequest request);

    void delete(Long edgeId);

    List<EdgeSuggestionDTO> generateSuggestions(Long projectId, GenerateEdgeRequest request);

    void batchSave(Long projectId, BatchSaveEdgesRequest request);

    List<EdgeDTO> getBySourceNode(String sourceNodeId);

    Long saveFromSuggestion(Long projectId, String sourceId, String targetId,EdgeSuggestionDTO suggestion);

    List<Long> saveSuggestionsAsEdges(Long projectId, String sourceId, String targetId,List<EdgeSuggestionDTO> suggestions);

    List<EdgeSuggestionDTO> generateAndSaveSuggestions(Long projectId, GenerateEdgeRequest request);
}
