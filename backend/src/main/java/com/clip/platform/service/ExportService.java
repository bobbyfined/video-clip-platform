package com.clip.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clip.platform.common.BusinessException;
import com.clip.platform.entity.ClipSuggestion;
import com.clip.platform.entity.TranscriptSegment;
import com.clip.platform.mapper.ClipSuggestionMapper;
import com.clip.platform.mapper.TranscriptSegmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 导出服务 - SRT / TXT
 */
@Service
@RequiredArgsConstructor
public class ExportService {

    private final TranscriptSegmentMapper segmentMapper;
    private final ClipSuggestionMapper clipMapper;

    /**
     * 导出 SRT 字幕格式
     */
    public String exportSrt(Long taskId) {
        List<TranscriptSegment> segments = segmentMapper.selectList(
                new LambdaQueryWrapper<TranscriptSegment>()
                        .eq(TranscriptSegment::getTaskId, taskId)
                        .orderByAsc(TranscriptSegment::getSortOrder));

        if (segments.isEmpty()) {
            throw new BusinessException(404, "没有找到转写数据");
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            TranscriptSegment seg = segments.get(i);
            sb.append(i + 1).append("\n");
            sb.append(formatSrtTime(seg.getStartSeconds()))
              .append(" --> ")
              .append(formatSrtTime(seg.getEndSeconds()))
              .append("\n");
            sb.append(seg.getText()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 导出 TXT 文本格式
     */
    public String exportTxt(Long taskId) {
        List<TranscriptSegment> segments = segmentMapper.selectList(
                new LambdaQueryWrapper<TranscriptSegment>()
                        .eq(TranscriptSegment::getTaskId, taskId)
                        .orderByAsc(TranscriptSegment::getSortOrder));

        if (segments.isEmpty()) {
            throw new BusinessException(404, "没有找到转写数据");
        }

        StringBuilder sb = new StringBuilder();
        for (TranscriptSegment seg : segments) {
            sb.append("[").append(formatTxtTime(seg.getStartSeconds())).append("] ");
            sb.append(seg.getText()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 导出切片建议 TXT
     */
    public String exportClipsTxt(Long taskId) {
        List<ClipSuggestion> clips = clipMapper.selectList(
                new LambdaQueryWrapper<ClipSuggestion>()
                        .eq(ClipSuggestion::getTaskId, taskId)
                        .orderByAsc(ClipSuggestion::getSortOrder));

        if (clips.isEmpty()) {
            throw new BusinessException(404, "没有找到切片建议");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== 视频切片建议 ===\n\n");
        for (int i = 0; i < clips.size(); i++) {
            ClipSuggestion clip = clips.get(i);
            sb.append(String.format("【切片 %d】%s\n", i + 1, clip.getTopic()));
            sb.append(String.format("  时间: %s --> %s\n", formatTxtTime(clip.getStartSeconds()), formatTxtTime(clip.getEndSeconds())));
            sb.append(String.format("  标题: %s\n", clip.getTitle1()));
            if (clip.getTitle2() != null) {
                sb.append(String.format("  备选标题: %s\n", clip.getTitle2()));
            }
            if (clip.getSummary() != null) {
                sb.append(String.format("  摘要: %s\n", clip.getSummary()));
            }
            if (clip.getHookReason() != null) {
                sb.append(String.format("  爆点: %s\n", clip.getHookReason()));
            }
            if (clip.getScore() != null) {
                sb.append(String.format("  评分: %d\n", clip.getScore()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 格式化 SRT 时间码: HH:MM:SS,mmm
     */
    private String formatSrtTime(double seconds) {
        int h = (int) (seconds / 3600);
        int m = (int) ((seconds % 3600) / 60);
        int s = (int) (seconds % 60);
        int ms = (int) ((seconds - Math.floor(seconds)) * 1000);
        return String.format("%02d:%02d:%02d,%03d", h, m, s, ms);
    }

    /**
     * 格式化 TXT 时间: HH:MM:SS
     */
    private String formatTxtTime(double seconds) {
        int h = (int) (seconds / 3600);
        int m = (int) ((seconds % 3600) / 60);
        int s = (int) (seconds % 60);
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
