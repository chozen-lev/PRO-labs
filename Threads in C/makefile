TARGET = lab4

.PHONY: all clean

all: $(TARGET)
	
clean:
	rm -rf $(TARGET) *.o

lab4.o: lab4.c
	gcc -o lab4.o -c lab4.c

$(TARGET): lab4.o
	gcc -pthread lab4.o -o $(TARGET)