package dan.dit.whatsthat.util;

/**
 * An OperationWaiter is useful as a lightweight notification
 * listener that can be called by any operation to notify a listener
 * that the operation is done.<br>
 * Another use is especially for async operations. When multiple operations
 * are required to having finished before operationDone() is invoked, this
 * can be assured by retrieving multiple instances of sub-listeners by makeSubOperation()
 * and using these created instances as listeners for the actual operations. The given OperationDoneListener
 * will only be invoked when the same number of operations finished as the amount of sub-listeners
 * previously created.<br>
 *     Example:
 *     OperationWaiter waiter = new OperationWaiter(new OperationDoneListener() {
 *        @Override
 *        public void operationDone() {
 *            //OperationOne and OperationTwo are both done, do stuff.
 *        }
 *     });
 *     OperationDoneListener[] subListener = new OperationDoneListener[] {waiter.makeSubOperation(), waiter.makeSubOperation()};
 *     startOperationOne(PARAMETERS, subListener[0]); // async
 *     startOperationTwo(PARAMETERS, subListener[1]); // async
 * Created by daniel on 03.04.15.
 */
public class OperationWaiter {
    private int mWaitCount;
    private OperationDoneListener mListener;

    /**
     * Creates an operation Waiter with a listener to invoke when
     * all sub-operations are done.
     * @param listener The listener to invoke.
     */
    public OperationWaiter(OperationDoneListener listener) {
        mListener = listener;
        if (mListener == null) {
            throw new IllegalArgumentException("Listener null.");
        }
    }

    /**
     * Creates a sub-listener to use for a sub-operation. Make all sub-listeners
     * before you start the operations to make sure that the listener associated with the Waiter
     * is only called when all operations are done. Else (especially for synchronous operations)
     * the Waiter's listener will probably be invoked multiple times.
     * @return A new sub-listener.
     */
    public OperationDoneListener makeSubOperation() {
        mWaitCount++;
        return new Listener();
    }

    /**
     * Notifies the waiter if the same amount of operations are done as previously
     * associated.
     */
    private class Listener implements OperationDoneListener {
        @Override
        public void operationDone() {
            if (mWaitCount > 0) {
                mWaitCount--;
                if (mWaitCount == 0) {
                    mListener.operationDone();
                }
            }
        }
    }
}
