import json
import re
import time
from pathlib import Path

from deep_translator import GoogleTranslator


ROOT = Path(r"Z:\My_mods\Z_Jobs")
EN_PATH = ROOT / "src" / "main" / "resources" / "assets" / "advancedjobs" / "lang" / "en_us.json"
RU_PATH = ROOT / "src" / "main" / "resources" / "assets" / "advancedjobs" / "lang" / "ru_ru.json"
CACHE_PATH = ROOT / "scripts" / ".ru_translate_cache.json"

PLACEHOLDER_RE = re.compile(r"%\d*\$?[sdf]|%s|%d|%f")
COMMAND_RE = re.compile(r"/[a-z_]+(?:\s+\[[^\]]+\]|\s+[a-z_<>|]+)*", re.IGNORECASE)


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def save_json(path: Path, data: dict) -> None:
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def is_broken(value: object) -> bool:
    return not isinstance(value, str) or "?" in value


def protect_tokens(text: str) -> tuple[str, dict[str, str]]:
    replacements: dict[str, str] = {}
    index = 0

    def replace_match(match: re.Match[str]) -> str:
        nonlocal index
        token = f"ZXTOK{index}ZX"
        replacements[token] = match.group(0)
        index += 1
        return token

    protected = PLACEHOLDER_RE.sub(replace_match, text)
    protected = COMMAND_RE.sub(replace_match, protected)
    return protected, replacements


def restore_tokens(text: str, replacements: dict[str, str]) -> str:
    restored = text
    for token, original in replacements.items():
        restored = restored.replace(token, original)
    return restored


def translate_text(text: str, translator: GoogleTranslator, cache: dict[str, str]) -> str:
    if text in cache:
        return cache[text]

    protected, replacements = protect_tokens(text)
    last_error: Exception | None = None
    for _ in range(3):
        try:
            translated = translator.translate(protected)
            if not isinstance(translated, str) or not translated.strip():
                translated = text
            translated = restore_tokens(translated, replacements)
            cache[text] = translated
            return translated
        except Exception as exc:  # pragma: no cover - network/service transient
            last_error = exc
            time.sleep(1.0)

    raise RuntimeError(f"Failed to translate text: {text}") from last_error


def main() -> None:
    en = load_json(EN_PATH)
    current_ru = load_json(RU_PATH)
    cache = load_json(CACHE_PATH) if CACHE_PATH.exists() else {}

    rebuilt = dict(en)
    for key, value in current_ru.items():
        if key in rebuilt and isinstance(value, str) and not is_broken(value):
            rebuilt[key] = value

    translator = GoogleTranslator(source="en", target="ru")
    translated_count = 0
    for key, en_value in en.items():
        ru_value = rebuilt.get(key)
        if not isinstance(en_value, str):
            rebuilt[key] = en_value
            continue
        if not isinstance(ru_value, str) or ru_value == en_value or is_broken(ru_value):
            rebuilt[key] = translate_text(en_value, translator, cache)
            translated_count += 1

    save_json(RU_PATH, rebuilt)
    save_json(CACHE_PATH, cache)
    print(f"Translated/updated {translated_count} entries.")


if __name__ == "__main__":
    main()
