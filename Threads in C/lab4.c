#include <stdio.h>
#include <pthread.h>
#include <semaphore.h>
#include <unistd.h>

#define FILE_LOGS "logs.txt"

#define MAX_BUFFER_SIZE 10
#define START_BUFFER_LENGTH 5

#ifdef FILE_LOGS
    #define printf(...) fprintf(pFile, __VA_ARGS__)
    FILE *pFile;
#endif

/* Потокові змінні */
pthread_t thread1;
pthread_t thread2;
pthread_t thread3;
pthread_t thread4;
pthread_t thread5;

/* Оголошення семафорів */
sem_t SCR1;
sem_t sem1;
sem_t sem2;

/* М'ютекс для захисту черги */
pthread_mutex_t MCR1 = PTHREAD_MUTEX_INITIALIZER;

int next = 0;
int CR1[MAX_BUFFER_SIZE];
int stack_length = 0;

int passageway = 0;

/* Додавання нового елементу до черги */
void push_stack(int value)
{
    CR1[stack_length++] = value;
}

/* Взяття чергового елементу з голови черги для обробки */
int pop_stack()
{
    /* Точно відомо, що черга не пуста */
    return CR1[--stack_length];
}

void* thread_producer(void* arg)
{
    /* Змінна для зберігання номера потоку */
    int num = *(int*)arg;

    /* Змінна для зберігання поточного значення лічильника семафора */
    int sem_value;

    while (1)
    {
        if (passageway == 4) {
            break;
        }

        if (pthread_mutex_trylock(&MCR1) == 0)
        {
            if (stack_length < MAX_BUFFER_SIZE)
            {
                push_stack(next++);

                sem_post(&SCR1);

                sem_getvalue(&SCR1, &sem_value);
                printf("Producer thread%d: semaphore=%d; element %d CREATED;\n", num, sem_value, CR1[stack_length - 1]);

                if(passageway == 0 && stack_length == MAX_BUFFER_SIZE) {
                    passageway++;
                }
                else if(passageway == 2 && stack_length == MAX_BUFFER_SIZE) {
                    passageway++;
                }
            }
            pthread_mutex_unlock(&MCR1);
        }
        else {
            printf("Thread%d waits for the mutex to open.\n", num);
        }
        usleep(1);
    }

    printf("Producer thread%d  stopped !!!\n", num);

    return NULL;
}

void* thread_consumer(void* arg)
{
    /* Змінна для зберігання номера потоку */
    int num = *(int*)arg;

    /* Оголошення вказівника curr_elem, який буде вказувати на поточний елемент з голови черги,
       для виконання над ним потрібних дій */
    int curr_elem = 0;

    /* Змінна для зберігання поточного значення лічильника семафора */
    int sem_value;

    while (1)
    {
        if (num == 1)
        {
            printf("Thread%d opens semaphore sem2\n", num);
            sem_post(&sem2);
            printf("Semaphore sem2 is opened!\n");
            printf("Thread%d waits for the opening of the semaphore sem1\n", num);
            sem_wait(&sem1);
            printf("Thread%d successfully passed semaphore sem1\n", num);
        }
        else if (num == 2)
        {
            printf("Thread%d opens semaphore sem1\n", num);
            sem_post(&sem1);
            printf("Semaphore sem1 is opened!\n");
            do {
                printf("Thread%d waits for the opening of the semaphore sem2\n", num);
            } while (sem_trywait(&sem2) == -1);
            printf("Thread%d successfully passed semaphore sem2\n", num);
        }

        /* Чекаємо доки семафор буде готовим. Якщо його значення більше 0,
         * то це означає, що черга не пуста, зменшуємо лічильник на 1.
         * Якщо черга пуста, то операція блокуєтся доти, доки в черзі не 
         * з'явиться хоч одне нове завдання */
        sem_wait(&SCR1);

        if (pthread_mutex_trylock(&MCR1) == 0)
        {
            curr_elem = pop_stack();

            sem_getvalue(&SCR1, &sem_value);
            printf("Consumer thread%d: semaphore=%d; element %d TAKEN;\n", num, sem_value, curr_elem);

            if(passageway == 1 && stack_length == 0) {
                passageway++;
            }
            else if(passageway == 3 && stack_length == 0) {
                passageway++;
                pthread_mutex_unlock(&MCR1);
                break;
            }

            /* Звільнення м'ютекса черги, оскільки доступ до 
             * спільного ресурсу (тобто черги) поки-що завершений */
            pthread_mutex_unlock(&MCR1); 
        }
        else {
            sem_post(&SCR1);
            printf("Thread%d waits for the mutex to open.\n", num);
        }
    }

    /* Відміна всіх інших потоків, оскільки завдання вичерпались
     * і постачальник припинив роботу */
    pthread_cancel(thread1);
    pthread_cancel(thread2);

    printf("Consumers thread1 and thread2  stopped !!!\n");

    return NULL;
}

int main()
{
#ifdef FILE_LOGS
    pFile = fopen(FILE_LOGS, "w");
#endif

    /* Оголошення змінних для нумерації потоків */
    int thread1_number = 1;
    int thread2_number = 2;
    int thread3_number = 3;
    int thread4_number = 4;
    int thread5_number = 5;

    /* Ініціалізація семафорів */
    sem_init(&SCR1, 0, 0);
    sem_init(&sem1, 0, 0);
    sem_init(&sem2, 0, 0);

    int sem_value;
    sem_getvalue(&SCR1, &sem_value);
    printf("SCR1 = %d\n", sem_value);

    for(next = 0; next < START_BUFFER_LENGTH; ++next)
    {
        push_stack(next);
        sem_post(&SCR1);
    }

    printf("Queue with elements from 0-th to %d-th has been created !!!\n", START_BUFFER_LENGTH - 1);

    sem_getvalue(&SCR1, &sem_value);
    printf("SCR1 = %d\n", sem_value);

    /* Кожному потоку передається вказівник на його номер, приведений до типу void*  */
    pthread_create(&thread1, NULL, &thread_consumer, (void*)&thread1_number);
    pthread_create(&thread2, NULL, &thread_consumer, (void*)&thread2_number);
    pthread_create(&thread3, NULL, &thread_producer, (void*)&thread3_number);
    pthread_create(&thread4, NULL, &thread_producer, (void*)&thread4_number);
    pthread_create(&thread5, NULL, &thread_producer, (void*)&thread5_number);

    /* Очікуємо завершення всіх потоків */
    pthread_join(thread1, NULL);
    pthread_join(thread2, NULL);
    pthread_join(thread3, NULL);
    pthread_join(thread4, NULL);
    pthread_join(thread5, NULL);

    printf("All threads stopped !!!\n");

#ifdef FILE_LOGS
    fclose(pFile);
#endif

    return 0;
}