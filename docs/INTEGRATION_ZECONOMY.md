# Z_Economy Integration

`Advanced Jobs` supports `Z_Economy` as an external provider.

## Design

The jobs mod talks to the public `Z_Economy` API instead of old internal implementation classes.

Bridge implementation:

- `src/main/java/com/example/advancedjobs/economy/ExternalEconomyBridge.java`

Used API types:

- `io.zicteam.zeconomy.api.ZEconomyApi`
- `io.zicteam.zeconomy.api.ZEconomyApiProvider`
- `io.zicteam.zeconomy.api.model.ApiResult`
- `io.zicteam.zeconomy.api.model.PlayerEconomySnapshot`

## What the bridge does

- `getBalance(UUID)`
  - reads wallet balance from `PlayerEconomySnapshot.walletBalances()`
- `deposit(UUID, amount, reason)`
  - calls `ZEconomyApi.addBalance(...)`
- `withdraw(UUID, amount, reason)`
  - calls `ZEconomyApi.addBalance(..., -amount)`

That means salary claim, tax routing, job change cost, and reroll cost can all flow through the same economy backend.

## Config

Use `config/ZAdvancedJobs/economy.json`:

```json
{
  "provider": "external",
  "externalCurrency": "z_coin",
  "taxSinkAccountUuid": "00000000-0000-0000-0000-000000000001"
}
```

## Runtime Behavior

If `Z_Economy` is not loaded:

- `Advanced Jobs` falls back to the internal economy provider

If `Z_Economy` is loaded:

- the external bridge becomes available
- balance, salary payout, tax routing, job-change payment, and reroll cost flow through `Z_Economy`

## Notes

- default external currency is `z_coin`
- if you still have an old `economy.json` with `sdm_coin`, update it manually
- the jobs mod only relies on the public API contract
- the jobs jar does not ship `Z_Economy` classes; the real API comes from the economy mod at runtime

## Recommended Verification

1. Start the server.
2. Check the `AdvancedJobs economy status` line in the log.
3. Confirm:
   - `configuredProvider=external`
   - `activeProvider=external`
   - `bridgeAvailable=true`
4. Claim salary in game.
5. Confirm the `Z_Economy` wallet balance changes.
