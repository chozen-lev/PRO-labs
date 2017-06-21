TARGET = lab5

.PHONY: all clean $(TARGET)

all: $(TARGET).jar

$(TARGET): $(TARGET).java
	javac $(TARGET).java

clean:
	rm -rvf $(TARGET).jar *.class

$(TARGET).jar: $(TARGET)
	jar -cfm $(TARGET).jar main.mn Main.class Producer.class Consumer.class CommonResource.class CommonVariables.class