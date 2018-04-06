import java.util.concurrent.locks.ReentrantLock;

public class GAThread{

   private int count = 0;
   private ReentrantLock lock = new ReentrantLock();

   private void increment() {
       for(int i = 0; i < 100000; i++) {
           count ++;
       }
   }

   public void firstThread() throws InterruptedException {
       lock.lock();

       try {
        increment();
       } finally {
        lock.unlock();
       }
   }

   public void secondThread() throws InterruptedException {
       lock.lock();
       
       try {
        increment();
       } finally {
        lock.unlock();
       }
   }

   public void finished() {
       System.out.println("Count is " + count);
   }

   public static void main(String[] args) throws Exception {
       final GAThread gat = new GAThread();

       Thread t1 = new Thread(new Runnable() {
           public void run() {
               try {
                   gat.firstThread();
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
           }
       });

       Thread t2 = new Thread(new Runnable() {
            public void run() {
                try {
                    gat.secondThread();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        gat.finished();

   }

}