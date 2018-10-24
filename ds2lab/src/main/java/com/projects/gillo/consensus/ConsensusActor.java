package com.projects.gillo.consensus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
	
	private List<Object> msgBuffer;
	
	public ConsensusActor(int id, GlobalClock clock, CrashFailure crashFailure) {
		this.id = id;
		this.clock = clock;
		this.crashed = false;
		this.phase1Waiting = false;
		this.msgBuffer = new ArrayList<>();
		this.crashFailure = crashFailure;
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
		this.round = -1;
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
		this.round += 1;
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
		System.out.println(this.msgBuffer.toString());
		for (Object msg : msgBuffer) {
			this.getSelf().tell(msg, null);
		}
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
			this.replyingProcesses = new HashSet<>();
			this.receivedValues = new HashSet<>();
			onStartRound();
		}
	}
	
	private void onPhase1Msg(Phase1Msg msg) {
		if (crashed)
			return;
		if (this.crashFailure == CrashFailure.ON_PHASE1) {
			this.crashed = true;
			this.isProcessDead();
		}
		if (this.round > msg.getRound()) {
			this.msgBuffer.add(msg);
			return;
		}
		if (this.coordinatorId != msg.getSenderId())
			return;
		System.out.printf("%d p%d p%d Received coordinator estimate = %s\n",
				System.currentTimeMillis(),
				this.id,
				msg.getSenderId(),
				this.estimate.toString());
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
		if (this.round > msg.getRound()) {
			this.msgBuffer.add(msg);
			return;
		}
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
				.build();
	}
	
	

}
