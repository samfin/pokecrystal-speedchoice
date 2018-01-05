AIChooseMove: ; 440ce
; Score each move in EnemyMonMoves starting from Buffer1. Lower is better.
; Pick the move with the lowest score.

; Wildmons attack at random.
	ld a, [wBattleMode]
	dec a
	ret z

	ld a, [wLinkMode]
	and a
	ret nz

; No use picking a move if there's no choice.
	callba CheckSubstatus_RechargeChargedRampageBideRollout
	ret nz


; The default score is 20. Unusable moves are given a score of 80.
	ld a, 20
	ld hl, Buffer1
rept 3
	ld [hli], a
endr
	ld [hl], a

; Don't pick disabled moves.
	ld a, [EnemyDisabledMove]
	and a
	jr z, .CheckPP

	ld hl, EnemyMonMoves
	ld c, 0
.CheckDisabledMove
	cp [hl]
	jr z, .ScoreDisabledMove
	inc c
	inc hl
	jr .CheckDisabledMove
.ScoreDisabledMove
	ld hl, Buffer1
	ld b, 0
	add hl, bc
	ld [hl], 80

; Don't pick moves with 0 PP.
.CheckPP
	ld hl, Buffer1 - 1
	ld de, EnemyMonPP
	ld b, 0
.CheckMovePP
	inc b
	ld a, b
	cp EnemyMonMovesEnd - EnemyMonMoves + 1
	jr z, .ApplyLayers
	inc hl
	ld a, [de]
	inc de
	and $3f
	jr nz, .CheckMovePP
	ld [hl], 80
	jr .CheckMovePP


; Apply AI scoring layers depending on the trainer class.
.ApplyLayers
	ld hl, TrainerClassAttributes + TRNATTR_AI_MOVE_WEIGHTS

	; If we have a battle in BattleTower just load the Attributes of the first TrainerClass (Falkner)
	; so we have always the same AI, regardless of the loaded class of trainer
	ld a, [InBattleTowerBattle]
	bit 0, a
	jr nz, .battle_tower_skip

	ld a, [TrainerClass]
	dec a
	ld bc, 7 ; Trainer2AI - Trainer1AI
	call AddNTimes

.battle_tower_skip
	lb bc, CHECK_FLAG, 0
	push bc
	push hl

.CheckLayer
	pop hl
	pop bc

	ld a, c
	cp 16 ; up to 16 scoring layers
	jr z, .DecrementScores

	push bc
	ld d, BANK(TrainerClassAttributes)
	predef FlagPredef
	ld d, c
	pop bc

	inc c
	push bc
	push hl

	ld a, d
	and a
	jr z, .CheckLayer

	ld hl, AIScoringPointers
	dec c
	ld b, 0
rept 2
	add hl, bc
endr
	ld a, [hli]
	ld h, [hl]
	ld l, a
	ld a, BANK(AIScoring)
	call FarCall_hl

	jr .CheckLayer

; Decrement the scores of all moves one by one until one reaches 0.
.DecrementScores
	ld hl, Buffer1
	ld de, EnemyMonMoves
	ld c, EnemyMonMovesEnd - EnemyMonMoves

.DecrementNextScore
	; If the enemy has no moves, this will infinite.
	ld a, [de]
	inc de
	and a
	jr z, .DecrementScores

	; We are done whenever a score reaches 0
	dec [hl]
	jr z, .PickLowestScoreMoves

	; If we just decremented the fourth move's score, go back to the first move
	inc hl
	dec c
	jr z, .DecrementScores

	jr .DecrementNextScore

; In order to avoid bias towards the moves located first in memory, increment the scores
; that were decremented one more time than the rest (in case there was a tie).
; This means that the minimum score will be 1.
.PickLowestScoreMoves
	ld a, c

.move_loop
	inc [hl]
	dec hl
	inc a
	cp NUM_MOVES + 1
	jr nz, .move_loop

	ld hl, Buffer1
	ld de, EnemyMonMoves
	ld c, NUM_MOVES

; Give a score of 0 to a blank move	
.loop2
	ld a, [de]
	and a
	jr nz, .skip_load
	ld [hl], a

; Disregard the move if its score is not 1	
.skip_load
	ld a, [hl]
	dec a
	jr z, .keep
	xor a
	ld [hli], a
	jr .after_toss

.keep
	ld a, [de]
	ld [hli], a
.after_toss
	inc de
	dec c
	jr nz, .loop2

; Randomly choose one of the moves with a score of 1 	
.ChooseMove
	ld hl, Buffer1
	call Random
	and 3
	ld c, a
	ld b, 0
	add hl, bc
	ld a, [hl]
	and a
	jr z, .ChooseMove

	ld [CurEnemyMove], a
	ld a, c
	ld [CurEnemyMoveNum], a
	ret
; 441af


AIScoringPointers: ; 441af
	dw AI_Basic
	dw AI_Setup
	dw AI_Types
	dw AI_Offensive
	dw AI_Smart
	dw AI_Opportunist
	dw AI_Aggressive
	dw AI_Cautious
	dw AI_Status
	dw AI_Risky
	dw AI_None
	dw AI_None
	dw AI_None
	dw AI_None
	dw AI_None
	dw AI_None
; 441cf


; ====================================================================================================
; too lazy to make a new file


SpecialTrainerStartOfBattleFar:
	ld a, [BattleType]
	cp BATTLETYPE_FISTFIGHT
	jp z, .fistfight
	cp BATTLETYPE_TELEKINESIS
	jp z, .telekinesis
	cp BATTLETYPE_WINTER
	jp z, .winter
	cp BATTLETYPE_JANINE_DISGUISE
	jp z, .janine_disguise
	cp BATTLETYPE_DISGUISE
	jp z, .disguise
	cp BATTLETYPE_PRESSURE
	jp z, .pressure
	cp BATTLETYPE_BURNHEAL
	jp z, .blaine
	cp BATTLETYPE_ATTRACT
	jp z, .attract
	cp BATTLETYPE_TRICKROOM
	jp z, .trickroom
	cp BATTLETYPE_SANDSTORM
	jp z, .sandstorm

	jp .done

.attract
	ld hl, AttractEffectStartText
	call StdBattleTextBox
	callba BattleCommand_MoveDelay
	jp .done

.trickroom
	ld hl, TrickRoomEffectStartText1
	call StdBattleTextBox
	callba BattleCommand_MoveDelay
	ld hl, TrickRoomEffectStartText2
	call StdBattleTextBox
	callba BattleCommand_MoveDelay
	jp .done

.fistfight
	ld hl, FistFightEffectStartText1
	call StdBattleTextBox
	callba BattleCommand_MoveDelay
	ld hl, FistFightEffectStartText2
	call StdBattleTextBox
	callba BattleCommand_MoveDelay
	jp .done

.telekinesis
	ld hl, TelekinesisEffectStartText1
	call StdBattleTextBox
	callba BattleCommand_MoveDelay
	ld hl, TelekinesisEffectStartText2
	call StdBattleTextBox
	callba BattleCommand_MoveDelay
	jp .done

.winter
	ld hl, WinterEffectStartText
	call StdBattleTextBox
	callba BattleCommand_MoveDelay
	jp .done

.blaine
	ld hl, BlaineEffectStartText
	call StdBattleTextBox
	jp .done

.pressure
	ld hl, PressureStartText
	call StdBattleTextBox
	callba BattleCommand_MoveDelay
	jp .done

.disguise
	ld hl, DisguiseEffectStartText
	call StdBattleTextBox
	jp .done

.janine_disguise
	ld hl, JanineEffectStartText
	call StdBattleTextBox
	jp .done

.sandstorm
	ld hl, SandstormEffectStartText
	call StdBattleTextBox
	callba BattleCommand_MoveDelay
	jp .done

.done
	call SpecialTrainerEffectFar
	ret

SpecialTrainerEffectFar: ; On enemy Switch in
	ld a, [BattleType]
	cp BATTLETYPE_WINTER
	jp z, .winter
	cp BATTLETYPE_DISGUISE
	jp z, .disguise
	cp BATTLETYPE_JANINE_DISGUISE
	jp z, .disguise
	cp BATTLETYPE_ATTRACT
	jp z, .attract
	cp BATTLETYPE_PRESSURE
	jp z, .pressure
	cp BATTLETYPE_BURNHEAL
	jp z, .blaine
	cp BATTLETYPE_SANDSTORM
	jp z, .sandstorm

	ret

.winter
	call WinterEffect
	ret

.attract
	call AttractEffect
	ret

.blaine
	call BlaineEffect
	ret

.pressure
	call RedEffect
	ret

.disguise
	call DisguiseEffect
	ret

.sandstorm
	call SandstormEffect
	ret

WinterEffect:
	ld hl, WinterEffectText
	call StdBattleTextBox
	callba BattleCommand_MoveDelay
	callba BattleCommand_ResetStats
	ret

AttractEffect:
	ld hl, AttractPreText
	call StdBattleTextBox

	callba BattleCommand_MoveDelay

	xor a
	ld [AttackMissed], a
	callba BattleCommand_Attract
	ret

DisguiseEffect:
	callba BattleCommand_Substitute
	ret

RedEffect:
	ld a, BATTLE_VARS_SUBSTATUS4_OPP
	call GetBattleVarAddr
	set SUBSTATUS_PRESSURE_RECHARGE, [hl]
	ret

BlaineEffect:
	ld a, BATTLE_VARS_STATUS_OPP
	call GetBattleVarAddr
	and a
	ret nz

	ld a, BATTLE_VARS_STATUS_OPP
	call GetBattleVarAddr
	set BRN, [hl]
	call UpdateOpponentInParty
	callba ApplyBrnEffectOnAttack
	ld de, ANIM_BRN
	call SetPlayerTurn
	callba Call_PlayBattleAnim_OnlyIfVisible
	call SetEnemyTurn

	ld hl, WasBurnedText
	call StdBattleTextBox

	callba UseHeldStatusHealingItem
	ret

SandstormEffect:
	ld a, [Weather]
	cp WEATHER_SANDSTORM
	ret z

	ld a, WEATHER_SANDSTORM
	ld [Weather], a
	ld a, 255
	ld [WeatherCount], a
	ld hl, SandstormBrewedText
	call StdBattleTextBox
	ret
