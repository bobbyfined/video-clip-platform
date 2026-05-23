package com.clip.platform.dto.response;

import com.clip.platform.entity.AnalysisResult;
import com.clip.platform.entity.ClipSuggestion;
import com.clip.platform.entity.TranscriptSegment;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskDetailResponse extends TaskResponse {
    private List<TranscriptSegment> segments;
    private AnalysisResult analysis;
    private List<ClipSuggestion> clips;
}
