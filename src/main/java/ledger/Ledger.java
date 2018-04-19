package ledger;

import messages.HeartBeat;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Ledger class is shared by all States and used to keep track
 * of all key-value entries that a node contains. It includes methods
 * to determine if a Ledger is out of sync with the cluster.
 * 
 * @author deenaariff
 *
 */
@Component
public class Ledger {

    // TODO: Many of the references to this object's variables were not referenced using the 'this' keyword. Need to fix.

	private Map<String,String> keyStore; // Map all keys to values
	private List<Log> logs; // Store A Growing List of Log Values
	private List<Log> updateQueue; // a Queue storing all new Logs entries between Heartbeat broadcasts
	private HashMap<Log,Integer> commitMap;
	private final int MAJORITYPLACEHOLDER = 7;
	
	/**
	 * The Constructor for the Ledger Class
	 * 
	 */
	public Ledger() {
		this.keyStore = new HashMap<String,String>();
		this.logs = new ArrayList<Log>();
		this.updateQueue = new ArrayList<Log>();
		this.commitMap = new HashMap<Log,Integer>();
	}

	/**
	 * A method to append new Log entries to the ledger. 
	 * <p>
	 * This allows a Leader to commit a log entry.
	 * 
	 * @param addition The new log to append to the Ledger.
	 */
	public void commitToLogs(Log addition) {
		logs.add(addition);
		// TODO: Need to search for followers' commits before updating key value store
		updateKeyStore(addition.getKey(),addition.getValue());
		updateQueue.add(addition);
	}

	/**
	 * Obtain commited data from the internal key-value store
	 *
	 * @param key
	 * @return
	 */
	public String getData(String key) {
		if(keyStore.containsKey(key)) {
			return keyStore.get(key);
		} else {
			throw new RuntimeException("This Key Has Not Been Commited to the Cluster");
		}
	}

	/**
	 * This method is called to add a new log to the update queue.
	 * During regular heartbeat intervals the queue is emptied
	 * and all logs in the queue are sent over the network to
	 * follower nodes. Confirmation messages are expected from
	 * a majority of follower nodes before the log will be
	 * "commited" to the leader's data store.
	 *
	 */
	public void addToQueue(Log value) {
		updateQueue.add(value);
	}

	/**
	 * This method allows us to commit a log after receiving a confirmation.
	 * The corresponding matching value to a log will be decremented in a hashmap.
	 * if the the value becomes 0, then we delete the key-value pair from the
	 * confirmation HashMap and commit the log to our internal data store in this
	 * leader node.
	 *
	 */
	public void receiveConfirmation(Log log) {
		if(commitMap.containsKey(log)) {
			Integer value = commitMap.get(log);
			if(value == 0) {
				commitMap.remove(log);
				commitToLogs(log);
			} else {
				commitMap.put(log,value-1);
			}
		}
	}

    public void receiveConfirmation(HeartBeat hb) {
//        if(commitMap.containsKey(log)) {
//            Integer value = commitMap.get(log);
//            if(value == 0) {
//                commitMap.remove(log);
//                commitToLogs(log);
//            } else {
//                commitMap.put(log,value-1);
//            }
//        }

        // TODO: What if the heartbeat we commit, over-writes previously written information from another heartbeat
        // Ex: HB1(1,2,3,4,5) HB2(1,2,3,4,5,6)
        // HB1 gets approved _after_ HB2
    }
	
	/**
	 * A method to return all new logs entries that have been queued in updateQueue List.
	 * Clears all entries from the updateQueue List. 
	 * 
	 * @return All logs that are stored in the member updateQueue Object.
	 */
	public List<Log> getUpdates() {
		List<Log> updates = new ArrayList<Log>();
		for(Log log : updateQueue) {
			commitMap.put(log,MAJORITYPLACEHOLDER); // TODO: Store the # which represents majority of nodes in file
			updates.add(log);
		}
		//updateQueue = new ArrayList<Log>();
		return updates;
	}
	
	/**
	 * Update the HashMap Data structure representing the Key-Value store
	 * 
	 * @param key The lookup key of the data to be entered.
	 * @param value The value mapped to the lookup key of the data being entered.
	 */
	private void updateKeyStore(String key, String value) {
        this.keyStore.put(key, value);
	}

    /**
     * This method is used by followers, and updates the ledger based on the
     * heartbeat. It iterates through all of the new commits sent from the
     * leader, and adds it to the logs and key store
     *
     * @param hb The heartbeat message sent from the leader
     */
    public void update(HeartBeat hb) {
        for(Log log : hb.getCommits()) {
            this.logs.add(log);
            updateKeyStore(log.getKey(), log.getValue());
        }
    }
	
	/**
	 * This members prints all current logs that have been stored in the ledger.
	 * 
	 */
	public void printLogs() {
		System.out.println("Logs:");
		for(Log entry : logs) {
			System.out.println("  " + entry.getIndex() + " | term: " + entry.getTerm());
		}
		System.out.println("\nKey-Value Pairs:");
		for (String key : keyStore.keySet()) {
			System.out.println("  " + key + " - " + keyStore.get(key));
		}
	}
	
	/**
	 * Tests for the Ledger class. 
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Ledger ledger = new Ledger();
		ledger.commitToLogs(new Log(1,2,"password","goats"));
		ledger.commitToLogs(new Log(2,3,"fire","hot"));
		ledger.printLogs();
	}
	
}
