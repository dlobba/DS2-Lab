package com.projects.gillo.consensus;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import scala.concurrent.duration.Duration;

public class ConsensusActor extends AbstractActor {
	
	public static enum CrashFailure {
		ON_PHASE1,
		ON_PHASE2,
		WHEN_COORDINATOR,
		NONE
	}

	public static class TickMessage implements Serializable {};
	public static class NextRoundMsg implements Serializable {};
	
	//private static final int PHASE1_TIMER = 5000;
	//private static final int PHASE2_TIMER = 5000;
	private static final int TICK = 500;

	private int id;
	private int round;
	private Object estimate;
	private boolean decided;
	private boolean stop;
	private int coordinatorId = -1;
	private boolean crashed;
	private boolean phase1Waiting;
	private CrashFailure crashFailure;
	
	private Map<Integer, ActorRef> participants;
	private Set<Integer> suspects;
	private Set<ActorRef> replyingProcesses;
	private Set<Object> receivedValues;
	
	private FailureDetector processFd;
	private FailureDetector globalFd;
	private GlobalClock clock;
	
	private Set<ActorRef> receivedNextRound;
	
	public ConsensusActor(int id, GlobalClock clock, CrashFailure crashFailure) {
		this.id = id;
		this.clock = clock;
		this.crashed = false;
		this.phase1Waiting = false;
		this.crashFailure = crashFailure;
		this.receivedNextRound = null;
	}
	
	static Props props(int id, GlobalClock clock, CrashFailure crashFailure) {
		return Props.create(ConsensusActor.class,
				() -> new ConsensusActor(id, clock, crashFailure));
	}
	
	private void onStartMessage(StartMessage msg) {
		this.participants = new HashMap<>();
		this.participants.putAll(msg.getParticipants());
		this.processFd = new FailureDetector(msg.getProcessFailureList());
		this.globalFd = new FailureDetector(msg.getGlobalFailureList());
		this.crashed = this.isProcessDead();
	}
	
	private void onProposeMsg(ProposeMsg msg) {
		if (crashed)
			return;
		this.round = 0;
		this.estimate = new Random().nextInt(Integer.MAX_VALUE);
		this.stop = false;
		this.decided = false;
		this.coordinatorId = this.round % this.participants.size();
		if (this.coordinatorId == this.id)
			this.estimate = msg.getProposal();
		this.replyingProcesses = new HashSet<>();
		this.receivedValues = new HashSet<>();
		this.onStartRound();
		this.sendTickMessageTimeout();
	}
	
	private void onStartRound() {
		if (crashed)
			return;
		if (stop)
			return;
		System.out.printf("%d p%d p%d Starting round %d\n",
				System.currentTimeMillis(),
				this.id,
				this.id,
				this.round);
		this.coordinatorId = this.round % this.participants.size();
		if (this.coordinatorId == this.id) {
			System.out.printf("%d p%d p%d Coordinator: p%d round: %d estimate = %s\n",
					System.currentTimeMillis(),
					this.id,
					this.id,
					this.id,
					this.round,
					this.estimate.toString());
			Phase1Msg msg = new Phase1Msg(this.round, this.estimate, this.id);
			if (this.crashFailure == CrashFailure.WHEN_COORDINATOR) {
				this.crashed = true;
				this.isProcessDead();// required to log info...
				return;
			}
			BBroadcast(msg);
			
		}
		this.phase1Waiting = true;
	}
	
	private void onPhase1End(Object aux) {
		if (crashed)
			return;
		System.out.printf("%d p%d p%d Broadcasting aux = %s\n",
				System.currentTimeMillis(),
				this.id,
				this.id,
				aux.toString());
		Phase2Msg msg = new Phase2Msg(this.round, aux, this.id);
		BBroadcast(msg);
	}
	
	private void onPhase2End() {
		if (crashed)
			return;
		Object estimate = new QUESTION();
		for (Object tmpEstimate : receivedValues) {
			if (!(tmpEstimate instanceof QUESTION))
				estimate = tmpEstimate;
		}
		if (!(estimate instanceof QUESTION))
			this.estimate = estimate;
		if (this.receivedValues.size() == 1 &&
				!(estimate instanceof QUESTION)) {
			DecideMsg msg = new DecideMsg(this.estimate, this.id);
			BBroadcast(msg);
		} else {
			this.receivedNextRound = new HashSet<>();
			BBroadcast(new NextRoundMsg());
			this.replyingProcesses = new HashSet<>();
			this.receivedValues = new HashSet<>();
			this.round += 1;
			// wait when it is safe to go to the next round
		}
	}
	
	private void onNextRoundMsg(NextRoundMsg msg) {
		if (this.receivedNextRound == null)
			return;
			// blocked receipt of this message
		if (!this.receivedNextRound.contains(this.getSender()))
			this.receivedNextRound.add(this.getSender());
	}
	
	private void onPhase1Msg(Phase1Msg msg) {
		if (crashed)
			return;
		if (this.crashFailure == CrashFailure.ON_PHASE1) {
			this.crashed = true;
			this.isProcessDead();
		}
		if (this.round != msg.getRound())
			return;
		if (this.coordinatorId != msg.getSenderId())
			return;
		System.out.printf("%d p%d p%d Received coordinator round %d estimate = %s\n",
				System.currentTimeMillis(),
				this.id,
				msg.getSenderId(),
				msg.getRound(),
				msg.getEstimate().toString());
		Object aux = msg.getEstimate();
		this.phase1Waiting = false;
		onPhase1End(aux);
	}
	
	private void onPhase2Msg(Phase2Msg msg) {
		if (crashed)
			return;
		
		if (this.crashFailure == CrashFailure.ON_PHASE2) {
			this.crashed = true;
			this.isProcessDead();
		}
		if (this.round > msg.getRound())
			return;
		this.receivedValues.add(msg.getEstimate());
		this.replyingProcesses.add(this.getSender());
		if (this.replyingProcesses.size() > this.participants.size() / 2)
			onPhase2End();
	}

	private void onDecideMsg(DecideMsg msg) {
		if (crashed)
			return;
		if (!this.decided) {
			System.out.printf("%d p%d p%d Decided estimate: %s at round %d\n",
					System.currentTimeMillis(),
					this.id,
					this.id,
					this.estimate.toString(),
					this.round);
			this.decided = true;
			this.stop = true;
		}
	}

	private void BBroadcast(Object msg) {
		for (ActorRef participant : this.participants.values()) {
			participant.tell(msg, this.getSelf());
		}
	}

	private boolean isProcessDead() {
		if (this.crashed)
			return true;
		long deadTime = this.globalFd.getFailureTimeOf(this.id);
		long currentTick = clock.getCurrentTick();
		if (currentTick >= deadTime) {
			System.out.printf("%d p%d p%d Crashed\n",
					System.currentTimeMillis(),
					this.id,
					this.id);
			return true;
		}
		return false;
	}
	
	private void onTickMessage(TickMessage msg) {
		if (crashed)
			return;
		long currentTick = clock.getCurrentTick();
		this.suspects = this.processFd
				.getSuspectsAt(currentTick);
		if (this.suspects.size() > this.participants.size() / 2) {
			System.out.printf("p%d: Too few processes survived, consensus not achievable.\n",
					this.id);
			return;
		}
		if (this.isProcessDead()) {
			this.crashed = true;
			return;
		}
		if (this.phase1Waiting && this.suspects.contains(this.coordinatorId)) {
			System.out.printf("%d p%d p%d Coordinator is suspected\n",
					System.currentTimeMillis(),
					this.id,
					this.id);
			this.phase1Waiting = false;
			this.onPhase1End(new QUESTION());
		}
		// if I'm waiting for NextRoundMsg
		if (this.receivedNextRound != null) {
			Set<ActorRef> suspects = new HashSet<>();
			for (Integer suspectid : this.suspects) {
				suspects.add(this.participants.get(suspectid));
			}
			Set<ActorRef> tmpParticipants = new HashSet<ActorRef>(this.participants.values());
			tmpParticipants.removeAll(suspects);
			// if all processes not crashed have sent the message
			// to go to the next round, then go to the next round
			if (tmpParticipants.containsAll(receivedNextRound)) {
				this.receivedNextRound = null;
				onStartRound();
			}
		}
		sendTickMessageTimeout();
	}
	
	private void sendTickMessageTimeout() {
		this.getContext()
		.getSystem()
		.scheduler()
		.scheduleOnce(Duration.create(TICK,
				TimeUnit.MILLISECONDS),
				this.getSelf(),
				new TickMessage(),
				getContext().system().dispatcher(),
				this.getSelf());
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(StartMessage.class, this::onStartMessage)
				.match(ProposeMsg.class, this::onProposeMsg)
				.match(Phase1Msg.class, this::onPhase1Msg)
				.match(Phase2Msg.class, this::onPhase2Msg)
				.match(DecideMsg.class, this::onDecideMsg)
				.match(TickMessage.class, this::onTickMessage)
				.match(NextRoundMsg.class, this::onNextRoundMsg)
				.build();
	}
}
