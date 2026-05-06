package org.azelabs.boxshare.application;

import org.azelabs.boxshare.application.interfaces.ICommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CommandInvoker {
    private final Map<String, ICommand<?, ?>> commands;

    @Autowired
    public CommandInvoker(List<ICommand<?, ?>> commands) {
        this.commands = commands.stream()
                .collect(Collectors.toMap(c -> c.getClass().getSimpleName(), Function.identity()));
    }

    public <T, R> R execute(String commandName, T request) {
        @SuppressWarnings("unchecked")
        ICommand<T, R> command = (ICommand<T, R>) this.commands.get(commandName);
        if (command == null) {
            throw new IllegalArgumentException("Unknown command found: " + commandName);
        }
        return command.handle(request);
    }
}
