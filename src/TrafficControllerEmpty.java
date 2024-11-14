import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TrafficControllerEmpty implements TrafficController {
	private final TrafficRegistrar registrar;
	private final Lock lock = new ReentrantLock(true);  // Fair lock (no starvation)
	private final Condition bridgeFree = lock.newCondition();
	private boolean bridgeOccupied = false;
	private int count = 0;

	public TrafficControllerEmpty(TrafficRegistrar registrar) {
		this.registrar = registrar;
	}

	@Override
	public void enterRight(Vehicle v) {
		lock.lock();
		try {
			while (bridgeOccupied || count >= 2) {
				bridgeFree.await();
			}
			count ++;
			registrar.registerRight(v);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void leaveLeft(Vehicle v) {
		lock.lock();
		try {
			registrar.deregisterLeft(v);
			count --;
			if( count == 0) {
				bridgeOccupied = false;
				bridgeFree.signal();
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void enterLeft(Vehicle v) {
		lock.lock();
		try {
			while (bridgeOccupied || count > 0) {
				bridgeFree.await();
			}
			bridgeOccupied = true;
			registrar.registerLeft(v);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void leaveRight(Vehicle v) {
		lock.lock();
		try {
			registrar.deregisterRight(v);
			bridgeOccupied = false;
			bridgeFree.signal();
		} finally {
			lock.unlock();
		}
	}
}
