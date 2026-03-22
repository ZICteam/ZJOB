import json
from pathlib import Path


ROOT = Path(r"Z:\My_mods\Z_Jobs")
RU_PATH = ROOT / "src" / "main" / "resources" / "assets" / "advancedjobs" / "lang" / "ru_ru.json"

UPDATES = {
    "gui.advancedjobs.profession_details": "О профессии",
    "gui.advancedjobs.tooltip.content_summary": "Действия: %s | Пассивы: %s | Ветки: %s",
    "gui.advancedjobs.tooltip.passive_preview": "Пассивы",
    "gui.advancedjobs.tooltip.passive_preview_line": "Ур. %s | %s",
    "gui.advancedjobs.tooltip.branch_preview": "Ветки навыков",
    "gui.advancedjobs.tooltip.branch_preview_line": "%s | Узлы: %s",
    "gui.advancedjobs.tooltip.reward_preview": "Примеры наград",
    "gui.advancedjobs.tooltip.reward_preview_line": "%s %s | $%s | %s XP",
    "gui.advancedjobs.profession_milestones": "Вехи: %s | Последняя: %s",
    "gui.advancedjobs.profession_unlocked_nodes": "Узлы навыков: %s",
    "gui.advancedjobs.profession_claim_share": "Следующая выплата: %s",
    "gui.advancedjobs.profession_effective_multiplier": "Множитель наград: x%s",
    "gui.advancedjobs.profession_daily_cycle": "Сброс ежедневок: %s",
    "gui.advancedjobs.profession_contract_cycle": "Ротация контрактов: %s",
    "gui.advancedjobs.profession_contract_reroll": "Реролл: $%s | откат %s",
    "gui.advancedjobs.next_passive_unlock": "Следующий пассив: %s",
    "gui.advancedjobs.next_passive_unlock_line": "Ур. %s | %s",
    "gui.advancedjobs.next_skill_point": "След. очко навыка: %s",
    "gui.advancedjobs.next_skill_node": "След. узел: %s",
    "gui.advancedjobs.next_skill_node_line": "%s | ур. %s | цена %s",
}


def main() -> None:
    data = json.loads(RU_PATH.read_text(encoding="utf-8"))
    data.update(UPDATES)
    RU_PATH.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
