package com.bdu.plotassistant.service;

import com.bdu.plotassistant.dto.request.*;
import com.bdu.plotassistant.dto.request.storynode.*;
import com.bdu.plotassistant.dto.response.*;
import com.bdu.plotassistant.dto.response.storynode.NodeDTO;
import com.bdu.plotassistant.dto.response.storynode.NodeDetailDTO;
import com.bdu.plotassistant.dto.response.storynode.NodeSummaryDTO;
import com.bdu.plotassistant.entity.StoryNode;

import java.util.List;

public interface StoryNodeService {

    String create(Long projectId, CreateNodeRequest request);

    List<NodeSummaryDTO> listByProject(Long projectId);

    NodeDetailDTO getDetail(String nodeId);

    void update(String nodeId, UpdateNodeRequest request);

    void delete(String nodeId);

    String generateDescription(String nodeId, GenerateNodeDescRequest request);

    void batchSave(Long projectId, BatchSaveNodesRequest request);

    List<NodeDTO> getByProject(Long projectId);

    NodeDTO getById(String nodeId);

    List<StoryNode> getNodesByProject(Long projectId);
    List<StoryNode> getNodesByAct(Long projectId, Integer actIndex);
    List<String> generateNodes(Long projectId, GenerateNodesRequest request);
    String generateNodeDescription(Long projectId, String nodeId, GenerateNodeDescRequest request);
}
