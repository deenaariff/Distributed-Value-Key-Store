package state_helpers;

import info.HostInfo;
import ledger.Ledger;
import ledger.Log;
import messages.HeartBeat;
import messages.Vote;
import routing.Route;
import rpc.rpc;
import voting_booth.VotingBooth;

import java.io.IOException;

public class Follower {

    /**
     * This method handles heartbeats received by the follower.
     *
     * If a proper heartbeat is received (from leader, and equal or lower term),
     * the follower confirms that the previous log items match then sends a
     * confirmation
     *
     * @param hb
     * @param ledger
     * @param host_info
     * @throws IOException
     */
    public static void HandleHeartBeat(HeartBeat hb, Ledger ledger, HostInfo host_info) throws IOException {
        if (!hb.hasReplied()) {  // Ensure this is a heartbeat from a leader (yet to be acknowledged)
            if (host_info.getTerm() <= hb.getTerm()) {  // Check if equal or behind leader term
                int prevIndex = hb.getPrevLogIndex();
                Log prevLogTerm = hb.getPrevLog();

                if (ledger.confirmMatch(prevIndex, prevLogTerm)) {  // Ensure prevLog Term matches at given index
                    System.out.println("[" + host_info.getState() + "]: PrevLogTerm in HeartBeat Matches");
                    ledger.update(hb);
                    hb.setReply(true);
                } else {
                    hb.setReply(false);
                }
            } else {  // False if term is ahead of leader term
                hb.setReply(false);
            }

            Route origin = hb.getRoute();

            // Update the origin info for the heartbeat on response
            hb.setTerm(host_info.getTerm());
            hb.setRoute(host_info.getRoute());

            if (hb.getReply()) {  // Ensure my commitIndex is synced
                ledger.syncCommitIndex(hb.getLeaderCommitIndex());
            }

            rpc.returnHeartbeat(hb, origin);  // Return heartbeat to the destination
        }
    }

    /**
     * This method handles incoming votes to the follower.
     *
     * If the vote is coming from a valid (higher term) candidate, it will cast
     * its vote (if it hasn't already) and return the vote.
     *
     * If it is not a valid candidate, return the vote without casting a vote.
     *
     * @param vote
     * @param vb
     * @param host_info
     */
    public static void HandleVote(Vote vote, VotingBooth vb, HostInfo host_info) throws IOException {
        if ((vote.getHostTerm() >= host_info.getTerm()) && !host_info.hasVoted()) {  // Check if valid candidate
            vote.castVote();
            host_info.setVote(vote.getRoute());
        }
        rpc.returnVote(vote);  // Send vote back
    }
}
