# Agent instructions

Follow `../EmptyPlugin/AGENTS.md` for the shared rules.

## Repo-specific rules

- Kite: the hook and the allay both carry the scoreboard tag `lom:kite`; remove the allay on every path where the hook is removed so none leak. `onFishhookRemove` casts `hookedEntity as Allay`; prefer `as? Allay` with a null check.
- Kite model is a CLAY_BALL whose `custom_model_data` equals the rod's value (1-100). Add new kites in craftengine-resources and keep that mapping.
- Each tweak lives in its own listener class (`Kite`, `Chicken`, `Silverfish`) registered in `SneakyEntityTweaks.onEnable`; don't mix tweaks. Registration is unconditional today, so any toggle needs a new config key.
