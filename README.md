# Recruitment task
Developed rest api that matches requirements provided in task description.<br>
Postman collection provided in ```setup``` directory.<br><br>
Docker compose for running localy whole project or just database provided in ```setup``` directory.<br>
By default application is initialized with some predefined data for configuration and testing purposes.<br>
This can be modified by in ```docker-compose.yml``` file located in ```setup``` directory by those envs:<br>
```yaml
    environment:
      - INITIALIZE_CONFIG_ON_STARTUP=true # if true then predefined configuration will be loaded for records processing priority
      - INITIALIZE_RECORDS_ON_STARTUP=true # if true then predefined records will be loaded
      - MONGO_URI=mongodb://mongo:27017
      - MONGO_DB_NAME=rankomat-app
      - HTTP_SERVER_PORT=8080
```

## Instruction to run manually:
### Option 1 - IDEA of choice:
1. Open project in IDEA of choice
2. Run docker-compose-local.yml
3. Run the project in IDEA
4. By default Http server starts on port 8080, MongoDB on port 27017

### Option 2 - Docker compose with contenerized application:
1. Run docker-compose.yml 
2. By default HTTP server starts on port 8080, MongoDB on port 27017