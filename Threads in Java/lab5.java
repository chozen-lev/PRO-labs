import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.Semaphore;
import java.util.concurrent.CyclicBarrier;
import java.io.FileWriter;
import java.io.IOException;

class Main
{
    public static final CommonResource CR1 = new CommonResource();
    public static final CommonVariables CR2 = new CommonVariables();

    public static final Semaphore Sem1 = new Semaphore(0, true);
    public static final Semaphore Sem2 = new Semaphore(0, true);

    public static final CyclicBarrier CB1 = new CyclicBarrier(2);
    public static final CyclicBarrier CB2 = new CyclicBarrier(2);

    public static void main(String args[])
    {
        new Consumer(1);
        new Consumer(2);
        new Producer(3);
        new Producer(4);
        new Producer(5);
    }
}

class CommonResource
{
    public static final int MaxBufSize = 100;
    public static final int MinBufSize = 0;
    int buf[] = new int[MaxBufSize];
    int length = 0;
    int next = 0;
    boolean IsEmpty = (length == MinBufSize);
    boolean IsFull = (length == MaxBufSize);

    synchronized int pop(int thread_number)
    {
        while (IsEmpty)
        {
            try {
                wait(); 
            }
            catch (InterruptedException e) {
                System.out.println("InterruptedException");
            }
        }

        int curr_elem = buf[--length];
        System.out.printf("Consumer thread%d: index %d; element %d TAKEN;\n", thread_number, length, curr_elem);
            
        IsEmpty = (length == MinBufSize);
        IsFull = false;
        
        notify();

        return curr_elem;
    }

    synchronized void push(int thread_number)
    {
        while (IsFull)
        {
            try {
                wait();
            }
            catch (InterruptedException e) {
                System.out.println("InterruptedException");
            }
        }

        buf[length++] = next++;
        System.out.printf("Producer thread%d: index %d; element %d CREATED;\n", thread_number, length-1, next-1);
        IsFull = (length == MaxBufSize);
        IsEmpty = false;
        
        notify();
    }
}

class CommonVariables
{
    private static byte varByte;
    private static short varShort;
    private static char varChar;
    private static int varInt;
    private static long varLong;
    private static float varFloat;
    private static double varDouble;
    private static boolean varBoolean;

    private static Random random = new Random();

    /* Оголошення м'ютекса (замка) */ 
    public static ReentrantLock mutex = new ReentrantLock();

    private FileWriter file;

    CommonVariables()
    {
        varByte = random.nextBoolean() ? (byte)random.nextInt(127) : (byte)(random.nextInt(127)-128);
        varShort = random.nextBoolean() ? (short)random.nextInt(32767) : (short)(random.nextInt(32767)-32768);
        varChar = (char)random.nextInt(255);
        varInt = random.nextInt();
        varLong = random.nextLong();
        varFloat = random.nextFloat();
        varDouble = random.nextDouble();
        varBoolean = random.nextBoolean();

        try
        {
            file = new FileWriter("logs.txt", false);

            file.write("CR2 updates in Main" + '\n');
            file.write("byte: " + varByte + '\n');
            file.write("short: " + varShort + '\n');
            file.write("char: " + (int)varChar + '\n');
            file.write("int: " + varInt + '\n');
            file.write("long: " + varLong + '\n');
            file.write("float: " + varFloat + '\n');
            file.write("double: " + varDouble + '\n');
            file.write("boolean: " + varBoolean + '\n');
            file.write("CR2 updated in Main" + '\n');

            file.flush();
        }
        catch(IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void updateVariables(int thread_number)
    {
        switch (thread_number)
        {
            case 3:
            {
                varByte = random.nextBoolean() ? (byte)random.nextInt(127) : (byte)(random.nextInt(127)-128);
                varShort = random.nextBoolean() ? (short)random.nextInt(32767) : (short)(random.nextInt(32767)-32768);
                varChar = (char)random.nextInt(255);
                varInt = random.nextInt();
                break;
            }
            case 4:
            {
                varLong = random.nextLong();
                varFloat = random.nextFloat();
                varDouble = random.nextDouble();
                varBoolean = random.nextBoolean();
                break;
            }
            case 5:
            {
                varChar = (char)random.nextInt(255);
                varInt = random.nextInt();
                varLong = random.nextLong();
                varFloat = random.nextFloat();
                break;
            }
        }
        fwrite(thread_number);
    }

    synchronized void fwrite(int thread_number)
    {
        try
        {
            file.write("CR2 updates in thread" + thread_number + '\n');
            file.write("byte: " + varByte + '\n');
            file.write("short: " + varShort + '\n');
            file.write("char: " + (int)varChar + '\n');
            file.write("int: " + varInt + '\n');
            file.write("long: " + varLong + '\n');
            file.write("float: " + varFloat + '\n');
            file.write("double: " + varDouble + '\n');
            file.write("boolean: " + varBoolean + '\n');
            file.write("CR2 updated in thread" + thread_number + '\n');

            file.flush();
        }
        catch(IOException ex) {
            System.out.println(ex.getMessage());
        } 
    }
}

class Consumer implements Runnable
{
    private int num;

    Consumer(int value)
    {
        num = value;
        new Thread(this).start();
    }
    
    public void run()
    {
        while (true)
        {
            if (num == 1)
            {
                try {
                    System.out.printf("Thread%d opens semaphore sem2\n", num);
                    Main.Sem2.release();
                    System.out.printf("Semaphore sem2 is opened!\n");
                    System.out.printf("Thread%d waits for the opening of the semaphore sem1\n", num);
                    Main.Sem1.acquire();
                    System.out.printf("Thread%d successfully passed semaphore sem1\n", num);
                } catch (InterruptedException e) {
                    System.out.println("Interrupted exception");
                }

                try {
                    System.out.printf("Thread%d waits for the CB1 to break up\n", num);
                    Main.CB1.await();
                    System.out.printf("Thread%d successfully passed barrier CB1\n", num);
                } catch (Exception e) { }
            }
            else if (num == 2)
            {
                try {
                    System.out.printf("Thread%d opens semaphore sem1\n", num);
                    Main.Sem1.release();
                    System.out.printf("Semaphore sem1 is opened!\n");
                    System.out.printf("Thread%d waits for the opening of the semaphore sem2\n", num);
                    Main.Sem2.acquire();
                    System.out.printf("Thread%d successfully passed semaphore sem2\n", num);
                } catch (InterruptedException e) {
                    System.out.println("Interrupted exception");
                }

                try {
                    System.out.printf("Thread%d waits for the CB1 to break up\n", num);
                    Main.CB1.await();
                    System.out.printf("Thread%d successfully passed barrier CB1\n", num);
                } catch (Exception e) { }
            }

            Main.CR1.pop(num);
        }
    }
}

class Producer implements Runnable
{
    private int num;

    Producer(int value)
    {
        num = value;
        new Thread(this).start();
    }
    
    public void run()
    {
        while (true)
        {
            if (num == 4 || num == 5) {
                try {
                    System.out.printf("Thread%d waits for the CB2 to break up\n", num);
                    Main.CB2.await();
                    System.out.printf("Thread%d successfully passed barrier CB2\n", num);
                } catch (Exception e) { }
            }

            Main.CR1.push(num);

            Main.CR2.mutex.lock();
            Main.CR2.updateVariables(num);
            Main.CR2.mutex.unlock();
        }
    }
}