#!/usr/bin/env python3
"""
FunASR 语音转写脚本
用法: python3 transcribe.py <audio_path> <output_path>

依赖: pip install funasr
"""
import sys
import json
import os

def transcribe(audio_path, output_path):
    try:
        from funasr import AutoModel
        from funasr.utils.postprocess_utils import rich_transcription_postprocess

        # 使用 Paraformer-zh 模型
        model = AutoModel(
            model="paraformer-zh",
            vad_model="fsmn-vad",
            punc_model="ct-punc",
            device="cpu",
        )

        result = model.generate(input=audio_path, batch_size_s=300)

        segments = []
        for i, item in enumerate(result):
            text = rich_transcription_postprocess(item["text"])
            # FunASR 返回的时间戳
            ts_list = item.get("timestamp", [])
            if ts_list and len(ts_list) >= 2:
                start_ms = ts_list[0][0]
                end_ms = ts_list[-1][1]
                segments.append({
                    "start": start_ms / 1000.0,
                    "end": end_ms / 1000.0,
                    "text": text,
                    "order": i + 1
                })
            else:
                segments.append({
                    "start": 0,
                    "end": 0,
                    "text": text,
                    "order": i + 1
                })

        output = {"segments": segments}
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(output, f, ensure_ascii=False, indent=2)

        # 输出到 stdout
        print(json.dumps({"success": True, "segment_count": len(segments)}))

    except ImportError:
        # FunASR 未安装时的降级处理
        print(json.dumps({"success": False, "error": "funasr 未安装，请执行: pip install funasr"}), file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(json.dumps({"success": False, "error": str(e)}), file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("用法: python3 transcribe.py <audio_path> <output_path>", file=sys.stderr)
        sys.exit(1)

    audio_path = sys.argv[1]
    output_path = sys.argv[2]

    if not os.path.exists(audio_path):
        print(json.dumps({"success": False, "error": f"文件不存在: {audio_path}"}), file=sys.stderr)
        sys.exit(1)

    transcribe(audio_path, output_path)
