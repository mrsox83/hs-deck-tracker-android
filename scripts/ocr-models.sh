#!/usr/bin/env bash
# Downloads PaddleOCR's PP-OCRv5 text detection and East Slavic text recognition models (Apache-2.0) and converts them
# to ONNX for the Cyrillic recognition. The workflow ocr-models.yml runs it and commits the result.
#   scripts/ocr-models.sh <output dir>
set -euo pipefail

out=$(realpath -m "${1:-app/src/main/assets/ocr}")
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
mkdir -p "$out"

python3 -m venv "$work/venv"
py="$work/venv/bin/python"
"$py" -m pip install -q paddlepaddle==3.2.2 paddle2onnx==2.1.0 onnx==1.17.0 onnxruntime==1.30.0 pyyaml

fetch() {
  local model=$1 dir="$work/$1"
  mkdir -p "$dir"
  for file in inference.json inference.pdiparams inference.yml; do
    curl -fsSL --retry 3 -o "$dir/$file" "https://huggingface.co/PaddlePaddle/$model/resolve/main/$file" && continue
    curl -fsSL --retry 3 "https://paddle-model-ecology.bj.bcebos.com/paddlex/official_inference_model/paddle3.0.0/${model}_infer.tar" |
      tar -x -C "$dir" --strip-components=1
    break
  done
}

convert() {
  "$work/venv/bin/paddle2onnx" --model_dir "$work/$1" --model_filename inference.json \
    --params_filename inference.pdiparams --save_file "$out/$2" --opset_version 14
}

fetch PP-OCRv5_mobile_det
fetch eslav_PP-OCRv5_mobile_rec
convert PP-OCRv5_mobile_det det.onnx
convert eslav_PP-OCRv5_mobile_rec rec_eslav.onnx

"$py" - "$work/eslav_PP-OCRv5_mobile_rec/inference.yml" "$out" <<'PY'
import sys, yaml, onnxruntime as ort
config = yaml.safe_load(open(sys.argv[1], encoding="utf-8"))
chars = config["PostProcess"]["character_dict"]
with open(f"{sys.argv[2]}/eslav_dict.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(chars) + "\n")
for name in ("det.onnx", "rec_eslav.onnx"):
    session = ort.InferenceSession(f"{sys.argv[2]}/{name}")
    print(name, [(i.name, i.shape) for i in session.get_inputs()], [(o.name, o.shape) for o in session.get_outputs()])
print("characters:", len(chars))
PY
ls -l "$out"
