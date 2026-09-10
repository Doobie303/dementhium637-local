# God Wars evidence follow-up — 2026-09-09

> Follow-up: the owner subsequently authorized custom definitions for unresolved gaps. See GOD_WARS_CUSTOM_RULES.md for implemented decisions and validation; the proposals below are the preserved research snapshot.

Research requested after step 3. This is a source review and recommended correction list, not an implementation release. No gameplay code, packed data or runtime classes changed, and no gameplay tests were needed. GOD_WARS_FIXES.md remains the record of what is currently staged.

## Findings and recommended decisions

The available sources can settle useful implementation decisions under the owner's approved OSRS fallback policy. They do not establish a complete, immutable 2011 specification. A dated guide, a mirror last edited in 2012, a modern wiki and an original Jagex statement have different evidential weight. Matching text on mirrors is not independent corroboration.

| Issue | Current implementation | Research conclusion / recommended follow-up |
| --- | --- | --- |
| Zilyana damage | Melee 315; magic 100–315 LP | Recommend OSRS fallback: melee max 270, magic 100–200. The magic interval has a named Jagex developer citation; 315/315 comes from a later, internally inconsistent mirror. This resolves a practical choice, not the exact historical conflict. |
| K'ril regular/special damage | Melee 490; smash 350–499 | Recommend OSRS fallback: melee max 460, smash 350–490. A pre-EoC mirror instead says 463/499; its general-dungeon article says melee 490. The mirror therefore does not supply one consistent authority for the currently mixed profile. The special interval has a named developer citation. |
| K'ril poison strength | Starts at 160 LP | RuneScape Wiki explicitly describes pre-EoC poison as 168, also present in the late pre-EoC mirror. Recommend 168 if retaining period-specific poison, with medium confidence; 160 is the coherent OSRS fallback. Neither finding establishes the 25% application chance. |
| K'ril prayer drain | Half current prayer, rounded down | Strong OSRS fallback support. The late pre-EoC mirror says half the damage dealt instead, so exact period arithmetic is still conflicting. Retain the current fallback unless better period evidence is recovered. |
| Zamorak bodyguards after boss death | Prefer last attacker/kill credit | Recommend each living bodyguard independently chooses an eligible random room player, matching the OSRS strategy page. Do not generalize this policy to all four gods. Exact 2011 behavior remains unverified. |
| Kree blue tornado defence | Ranged accuracy/defence; magic damage/protection | Direct Jagex archival support for using ranged defence. Keep this behavior. The official evidence specifically confirms the defender-side stat; it does not by itself establish every part of the attacker's accuracy formula. |
| Kree melee defence and number of hits | Ordinary melee accuracy; one hit, max 260 | Modern wiki-derived data labels it magical melee; a later emulator author attributes magic-defence behavior to Mod Ash, but the original statement was not recovered. Recommend treating magic-defence melee as an OSRS fallback candidate, not an independently verified 2011 rule. Do not add a second hit from the disputed mirror alone. |
| Kree melee selection | Automatically chooses melee when untargeted | Sources establish eligibility when untargeted, but not that every untargeted attack must be melee. A later recreation explicitly describes continued ranged/magic selection; that is a research lead, not proof. Current automatic selection remains a simplification. |
| Zilyana/Kree style odds; poison odds | 50/50, 50/50, 25% | No authoritative numerical rates located. Preserve as explicitly provisional values; do not present passing probability tests as historical verification. |
| Visuals and knockback | Native definitions exist; clipped push | Written descriptions establish intended cues, not exact 637 IDs, delays, displacement distribution or duration. Cache existence does not prove the animation identity. Live comparison against clearly dated footage remains necessary. |
| Hidden stats / Strength drains / curses | OSRS fallback stats; fixed damage caps; partial reset | This search did not recover a 2011 server stat dump or encounter-specific drain contract. Strength-drain scaling and percentage-curse reset are implementation follow-ups, not resolved by a boss wiki table. |

## Stronger sources

1. **Original Jagex poll archive:** [Old School Content Poll #34, June 2015](https://oldschool.runescape.com/polls/2015/1257). Its Kree question explicitly describes ranged defence as the existing magic-attack behavior since GWD's release, then proposes changing it. The returned results show 62.9% yes; do not interpret the presence of a proposal as evidence it was implemented. This is direct publisher-hosted historical evidence, although the page is from OSRS rather than a 2011 RuneScape code release.

2. **Named Jagex developer statements preserved by the wiki:** [K'ril](https://oldschool.runescape.wiki/w/K%27ril_Tsutsaroth) references Mod Ash on 24 October 2018 for Zilyana/K'ril magic intervals, and 7 April 2020 for K'ril's uniformly distributed special damage and the melee branch's inclusion of its enraged attack. [Zilyana](https://oldschool.runescape.wiki/w/Commander_Zilyana) carries the same magic citation. These support the OSRS fallback values after multiplying by ten for server LP. The original social-media posts were not independently retrieved; this is developer evidence as reproduced by the wiki. The wiki's melee cap is a separate data-table claim, not automatically covered by the magic statement. No more than the cited statement's actual scope should be inferred.

3. **Explicit historical note:** [RuneScape Wiki — K'ril](https://runescape.wiki/w/Kril_Tsutsaroth) identifies 168 poison damage as pre-EoC. Its current fight description includes later mechanics and must not otherwise be copied wholesale. This narrows the poison-strength question, but does not prove the exact 2011 rate, timing or decay sequence.

4. **Specific bodyguard behavior:** [OSRS K'ril strategy guide](https://oldschool.runescape.wiki/w/K%27ril_Tsutsaroth/Strategies) states random post-death targets for the minions. It also describes the prayer smash's current-prayer drain. Use this as the owner-approved fallback, without importing later shield passives, equipment or access requirements.

## Sources that need less weight than the earlier report gave them

- [Darkan Zilyana](https://wiki.darkan.org/Commander_Zilyana) is visibly last edited 17 November 2012. Its infobox claims a 4.8-second attack interval while its body describes rapid knife-like attacks. Its 315/315 damage assertion is real, but this is not a certified 2011 snapshot. That weakens the reason for retaining it over the OSRS fallback. The inconsistent speed may be a template problem; it does not prove that every number on the page is wrong.
- [Darkan K'ril](https://wiki.darkan.org/K%27ril_Tsutsaroth) gives 463/499/300 and poison 168. [Its dungeon overview](https://wiki.darkan.org/God_wars_dungeon) instead gives 490 regular melee and lower magic claims. [Its strategy page](https://wiki.darkan.org/K%27ril_Tsutsaroth/Strategies) explicitly includes a September 2012 change. None of these establishes that current 490 regular melee is the correct 2011 value.
- [Darkan Kree strategies](https://wiki.darkan.org/Kree%27arra/Strategies) describes two 250 hits, but also includes the September 2012 access change. The [RuneHQ guide](https://www.runehq.com/special/god-wars-dungeon) now describes EoC-sized damage. Their agreement that two hits can occur does not date that behavior to 637.
- [2008 Saradomin guide on Tip.It](https://forum.tip.it/topic/185568-saradomins-encampment-guide/) is useful contemporary player testimony for kiting, target switching and last-hit bodyguard focus after Zilyana's death. It is not server code and does not certify exact rates. This supports keeping per-god behavior rather than applying Zamorak's random rule everywhere.
- [A 2018 recreation discussion](https://rune-server.org/threads/recreating-the-game-that-i-loved-2011-recreation-server.678889/page-4) attributes mixed Kree defence to Mod Ash and describes non-guaranteed melee selection while untargeted. It openly guesses a displacement proc rate. It is a lead for further primary evidence, not a source from which to copy probability constants.
- [Current Kree wiki index](https://osrsindex.com/wiki/kree-arra?site=osrs_wiki) reproduces the magical-melee classification. It is not independent confirmation of the underlying wiki and supplies no recovered 2011 formula.

## Archive coverage and limits

Jagex does retain accessible [official historical polls](https://oldschool.runescape.com/polls/2013), and its [published drop-rate page](https://www.runescape.com/drop-rates) contains GWD information. The latter addresses rewards/current RuneScape, not the unresolved 2011 combat rates; rewards remain outside this work.

Searched both wikis, dated fansite/forum guides, indexed Jagex developer references, original runescape.com poll pages, and archive routes. A timestamp-bounded RuneScape Wiki revision API request and a Wayback CDX request for the 2011 Zilyana page could not be retrieved by the browsing service. This is an access limitation, not evidence that those revisions never existed. No public Jagex archive exposing a full 2011 NPC combat implementation or the remaining exact probabilities was located. No messages were sent to Jagex or other people.

## Proposed next batch

1. Adopt the better-supported OSRS fallback damage intervals for Zilyana and K'ril, while recording any retained pre-EoC exceptions explicitly (not mixing values accidentally).
2. Give Zamorak's followers their own post-death target policy.
3. Review Kree's melee defence/selection and visuals with targeted evidence and in-game comparison; keep one melee hit until stronger period evidence supports a second.
4. Keep unverified proc odds labelled as assumptions. Add exact values only when supported, and separate combat drain/reset fixes from historical source questions.

No automatic claim is made that every uncertainty is now closed. The clear gain is a stronger basis for several practical corrections, one publisher-confirmed existing mechanic, and a more honest boundary around the unresolved original behavior.
