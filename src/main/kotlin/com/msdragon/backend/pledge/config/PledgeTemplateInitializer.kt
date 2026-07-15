package com.msdragon.backend.pledge.config

import com.msdragon.backend.pledge.entity.PledgeTemplate
import com.msdragon.backend.pledge.repository.PledgeTemplateRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PledgeTemplateInitializer(
	private val pledgeTemplateRepository: PledgeTemplateRepository,
) : ApplicationRunner {
	@Transactional
	override fun run(args: ApplicationArguments) {
		val existingContents = pledgeTemplateRepository.findAll().mapTo(mutableSetOf(), PledgeTemplate::content)
		val missingTemplates = DEFAULT_CONTENTS
			.filterNot(existingContents::contains)
			.map(::PledgeTemplate)
		if (missingTemplates.isNotEmpty()) {
			pledgeTemplateRepository.saveAll(missingTemplates)
		}
	}

	companion object {
		private val DEFAULT_CONTENTS = listOf(
			"\"아직 멀었어?\" 반복하기 금지",
			"\"돈 아깝다\" 말하기 금지",
			"\"아무거나\" 해놓고 나중에 불평하기 금지",
			"\"겨우 이거 보러 왔어?\" 말하기 금지",
			"\"이거 한국 돈으로 얼마야?\" 반복하기 금지",
			"\"내가 알아서 할게\" 하고 무리하기 금지",
			"\"사진 좀 그만 찍어\" 짜증내기 금지",
			"\"조금만 더 가면 돼\"라고 속이기 금지",
			"\"다시는 같이 안 와\" 말하기 금지",
			"피곤한데 괜찮은 척하기 금지",
			"\"밥 아직 멀었어?\" 반복하기 금지",
			"\"화장실은 아까 갔잖아\" 말하기 금지",
			"\"왜 이렇게 천천히 걸어?\" 재촉하기 금지",
			"\"그냥 빨리 가자\" 재촉하기 금지",
			"\"여기 별로다\" 도착하자마자 말하기 금지",
			"\"너무 덥다\" 또는 \"너무 춥다\" 계속 말하기 금지",
			"\"여기 맛집 맞아?\" 압박하기 금지",
			"\"이건 무슨 맛으로 먹어?\" 타박하기 금지",
			"\"너희끼리 정해\" 해놓고 불만 갖기 금지",
			"\"나는 괜찮다\" 하고 진짜 마음 숨기기 금지",
			"\"사진 다시 찍어줘\" 무한 반복하기 금지",
			"\"엄마·아빠는 몰라도 돼\" 말하기 금지",
			"\"그거 아니야, 이렇게 해야지\" 잔소리하기 금지",
			"\"검색해봤는데 여긴 아닌데?\" 현장에서 반박하기 금지",
			"\"계획이 왜 이래?\" 따지기 금지",
			"\"그냥 패키지로 갈걸\" 말하기 금지",
			"\"집이 제일 편하다\" 반복하기 금지",
			"라면으로 식사를 대충 넘기기 금지",
			"\"이 돈이면 집에서 먹지\" 말하기 금지",
			"\"요즘 사람들은 이런 데를 좋아해?\" 세대 잔소리하기 금지",
			"\"부모님 모시고 왔으면 좀 알아서 해\" 말하기 금지",
			"\"내가 말했잖아\" 반복하기 금지",
			"\"빨리빨리\" 재촉하기 금지",
			"\"괜찮다니까\" 하고 아픈 것 숨기기 금지",
			"\"여기까지 왔는데 해야지\" 하며 강행하기 금지",
			"\"사진 이상하게 나왔어\" 타박하기 금지",
			"\"좀 웃어봐\" 강요하기 금지",
			"\"내가 운전했는데 왜 불평이야\" 말하기 금지",
			"\"내가 예약했으니까 그냥 따라와\" 말하기 금지",
			"\"옛날에는 이 정도 걸었어\" 하며 무리하기 금지",
			"\"나 때문에 망친 것 같아\" 자책하기 금지",
			"\"그냥 숙소 가자\" 갑자기 포기 선언하기 금지",
			"\"다음부터 너랑 안 와\" 말하기 금지",
			"\"누가 여기 오자고 했어?\" 책임 묻기 금지",
			"\"왜 그때 말을 안 했어?\" 뒤늦게 추궁하기 금지",
		)
	}
}
