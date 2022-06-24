
from email.policy import default
import cx_Oracle
import json
#Poderá ser necessário por causa de problema relacionado com a serialização do DateTime
import bson # import json_util 
import requests
import pymongo

#pip install requests
#pip install pymongo

# --------------------------------------------------------------------------------------------------------------------------- #

# ** FILIPA: Usar python do windows **

#-> Download da Pasta: https://www.oracle.com/pt/database/technologies/instant-client/winx64-64-downloads.html
#-> pip install cx-Oracle
#-> Código a utilizar para a conexão c/ caminho para a pasta: cx_Oracle.init_oracle_client(lib_dir=r"C:\Users\Filipa Pereira\Desktop\instantclient_21_3")
#-> Código a utilizar para a conexão c/ caminho para a pasta: cx_Oracle.init_oracle_client(lib_dir=r"C:\Users\LuisPinto\Desktop\NoSQL\Grupo_08_NoSQL\instantclient_21_3") 


# ** LUISA: Usar python do Anaconda **
#-> Download da Pasta: https://www.oracle.com/pt/database/technologies/instant-client/winx64-64-downloads.html
#-> pip install cx-Oracle
#-> Copiar todos ficheiros *.dll da pasta descarregada para a pasta do Anaconda que contém o python.exe 

# --------------------------------------------------------------------------------------------------------------------------- #

# Se for caso Filipa descomentar
# cx_Oracle.init_oracle_client(lib_dir=r"C:\Users\Filipa Pereira\Desktop\instantclient_21_3")
# cx_Oracle.init_oracle_client(lib_dir=r"C:\Users\LuisPinto\Desktop\NoSQL\Grupo_Project\instantclient_21_3")


# CREDENCIAS DE CONEXÃO - ** Atenção ** Colocar Credenciais Correta
user= 'HR'
pwd = 'HR2022'
host = 'localhost' 
service_name = 'orclpdb1.localdomain'
portno = 1521 

# FUNÇÃO: Remove os campos a null do dicionário que armazena o contéudo do ficheiro JSON
# PODE SER UTILIZADO PARA QUALQUER DICIONÁRIO - Acho - Função retirada do stackOverflow
def deleteNullFromDict(d):

    for key, value in list(d.items()):
        if value is None:
            del d[key]
        elif isinstance(value, dict):
            deleteNullFromDict(value)
        elif isinstance(value, list):
            for elem in value:
                deleteNullFromDict(elem)
    return d

#FUNÇÃO: Constroí o dicionário (conteudo JSON) que representa a location, region e country do departamento
def buildDepartmentLocation(dictFile):

    # ------------- INFORMAÇÃO SOBRE A LOCALIZAÇÃO ---------------------
    cursor.execute("SELECT * FROM LOCATIONS WHERE LOCATION_ID = :local", local=locationID)

    resultLocation = cursor.fetchall()

    location = resultLocation[0]

    #Atributos da localização
    street = location[1]
    postal = location[2]
    city = location[3]
    state = location[4]
    countryID = location[5]

    # dicionário com a informação sobre a localização
    dictLocation = {"Street Address": street, "Postal Code": postal, "City": city, "State Providence": state}

    # ------------------ INFORMAÇÃO SOBRE COUNTRY ------------------------
    cursor.execute("SELECT * FROM COUNTRIES WHERE COUNTRY_ID = :country", country=countryID)

    resultCountries = cursor.fetchall()

    country = resultCountries[0]

    #Atributos do país
    countryName = country[1]
    regionID = country[2]

    # ------------------ INFORMAÇÃO SOBRE REGION ------------------------
    cursor.execute("SELECT * FROM REGIONS WHERE REGION_ID = :region", region=regionID)

    resultRegions = cursor.fetchall()

    #Atributos da região
    region = resultRegions[0]

    regionName = region[1]

    # ------------------ ARMAZENAMENTO DA INFORMAÇÃO EM JSON ------------------------

    # Como a localização tem mais informação do que a região e país então esta informação encontra-se aninhada 
    dictFile = {"Department": departmentName, "Region": regionName,  "Country": countryName, "Location": {}}
    dictFile["Location"].update(dictLocation)

    return dictFile
        
#FUNÇÃO: Constroí o dicionário (conteudo JSON) que representa o manager do departamento
def buildDepartmentManger(dictFile):

    cursor.execute("SELECT * FROM EMPLOYEES WHERE EMPLOYEE_ID = :manager", manager=managerID)

    resultManager = cursor.fetchall()

    # Caso haja manager na lista de employees
    if not (not resultManager): 

        manager = resultManager[0]

        #Atributos do Employee
        firstName = manager[1]
        lastName = manager[2]
        email = manager[3]
        phoneNumber = manager[4]
        hireDate = manager[5]
        jobId = manager[6]
        salary = manager[7]
        commision = manager[8]
        mangId = manager[9]
        depId = manager[10]

        # Armazenamento da informação do Manager
        dictManger = {"Manager": {"First Name":firstName, "Last Name": lastName, "Email": email, "Phone Number": phoneNumber, "Hire Date": hireDate, 
                                    "Salary": salary, "Comission": commision, "Job": {}}}

        cursor.execute("SELECT * FROM JOBS WHERE JOB_ID= :job", job=jobId)

        resultJob = cursor.fetchall()

        #Trabalho do manager
        job = resultJob[0]

        #Atributos do Job
        title = job[1]
        minSalary = job[2]
        maxSalary = job[3]

        dictJob = {"Title":title, "Minimum Salary": minSalary, "Maximum Salary": maxSalary}

        #Adicionar o job ao manager
        dictManger["Manager"]["Job"].update(dictJob)
        dictFile.update(dictManger)

        return dictFile

#FUNÇÃO: Constroí o dicionário (conteudo JSON) que representa os empregos e employees do departamento
def buildDepartmentJobs(dictFile):

    cursor.execute("SELECT * FROM EMPLOYEES WHERE DEPARTMENT_ID = :department", department=IdDepartment)

    resultEmployees = cursor.fetchall()

    #Armazena Temporáriamente o ID do job e o seu conteudo em JSON como uma parte ou seja jobs = [ (id, JSON), ....]
    # Este armazenamento foi necessário pois é possivel que mais do que um employee facam o msm job. Desta forma mantemos track dos jobs que podem existir
    jobs = []
    dictJobsTotal = {}

    # Caso haja employees a trabalhar no departamento
    if not (not resultEmployees):

        for employee in resultEmployees:

            #Atributos do Employee
            firstName = employee[1]
            lastName = employee[2]
            email = employee[3]
            phoneNumber = employee[4]
            hireDate = employee[5] 
            jobId = employee[6]
            salary = employee[7]
            commision = employee[8]
            mangId = employee[9]
            depId = employee[10]

            #Devolvenos os ID dos jobs da lista temporária
            firstElems = [x[0] for x in jobs]

            dictEmployee = {"First Name":firstName, "Last Name": lastName, "Email": email, "Phone Number": phoneNumber, "Hire Date":  hireDate, 
                                    "Salary": salary, "Comission": commision} 

            #Se o Id ainda não está na lista então significa que por agora só há um employee a fazer esse trabalho
            if jobId not in firstElems:

                cursor.execute("SELECT * FROM JOBS WHERE JOB_ID= :job", job=jobId)

                resultJob = cursor.fetchall()

                job = resultJob[0]

                #Atributos do Job
                title = job[1]
                minSalary = job[2]
                maxSalary = job[3]

                dictJob = {"Title":title, "Minimum Salary": minSalary, "Maximum Salary": maxSalary,"Employees": []}
                #Adiciona o employee a lista de employees que particam o job
                dictJob["Employees"].append(dictEmployee)

                #atualiza a lista temporária
                jobs.append((jobId, dictJob))

            else: # Caso o id já exsita na lista então significa que há +1 employee a particar o job

                for i in range(len(jobs)):

                    if jobs[i][0] == jobId:

                        #Atualiza o conteudo JSON com o novo employee encontrado que pratica o job com ID = jobId.
                        jobs[i][1]["Employees"].append(dictEmployee)

    # Caso o departamento tenha employees a trabalhar nele
    if len(jobs) >= 1:

        #Criação de uma nova secção 'Jobs' no JSON
        dictJobsTotal  = {"Jobs": []}

        for elem in jobs:

            dictJobsTotal["Jobs"].append(elem[1])

    #Atualiza o counteudo JSON final
    dictFile.update(dictJobsTotal)
    
    return dictFile

#FUNÇÃO: Constroí o dicionário (conteudo JSON) que representa historico do departamento
def buildDepartmentHistoric(dicFile):

    cursor.execute("SELECT * FROM JOB_HISTORY WHERE DEPARTMENT_ID= :department", department=IdDepartment)

    resultHistoric = cursor.fetchall()

    dictHistoric = {}
    
    #Armazena Temporáriamente o ID do employee e o seu conteudo em JSON como uma parte ou seja employeesHistoric = [ (id, JSON), ....]
    # Este armazenamento foi necessário pois é possivel que um employee tenha realizado dois jobs num msm departamento. 
    # Desta forma mantemos track dos employees que podem existir e respetivos jobs
    employeesHistoric = []

    # Caso haja historico associado ao departamento
    if not (not resultHistoric):

        for historic in resultHistoric:

            #Atributos do historico
            employeeId = historic[0]
            startDate = historic[1]
            endDate = historic[2]
            IdJob = historic[3]

            cursor.execute("SELECT * FROM EMPLOYEES WHERE EMPLOYEE_ID = :employee", employee=employeeId)

            resultEmployee = cursor.fetchall()

            employee = resultEmployee[0]

            #Atributos do Employee
            firstName = employee[1]
            lastName = employee[2]
            email = employee[3]
            phoneNumber = employee[4]

            cursor.execute("SELECT * FROM JOBS WHERE JOB_ID= :job", job=IdJob)

            resultJob = cursor.fetchall()

            #Job realizado no passado pelo o employee
            job = resultJob[0]

            #Atributos do Job
            title = job[1]
            minSalary = job[2]
            maxSalary = job[3]

            dictWorkedIn = { "Start Date" :  startDate, "End Date": endDate, "Job": {}}
            dictJobHistoric = {"Title":title, "Minimum Salary": minSalary, "Maximum Salary": maxSalary}
            dictWorkedIn["Job"].update(dictJobHistoric)

            #Devolvenos os ID dos employees da lista temporária
            firstElems = [x[0] for x in employeesHistoric]

            #Se o Id ainda não está na lista então significa que por agora só há um job que foi realizado pelo employee
            if employeeId not in firstElems:

                dictEmployeeHistocic = {"First Name":firstName, "Last Name": lastName, "Email": email, "Phone Number": phoneNumber,"Worked in": []}
                dictEmployeeHistocic["Worked in"].append(dictWorkedIn) #Adiciona à lista o trabalho realizado no passado pelo o employee

                employeesHistoric.append((employeeId,dictEmployeeHistocic))
            else: # Caso o id já exsita na lista então significa que o employee realizou +1 job no msm departamento

                for i in range(len(employeesHistoric)):

                    if employeesHistoric[i][0] == employeeId:

                        #Atualiza o conteudo JSON com o novo job encontrado que foi praticado pelo o employee com ID = employeeId.
                        employeesHistoric[i][1]["Worked in"].append(dictWorkedIn)

        # Caso o departamento tenha historico
        if len(employeesHistoric) >= 1:

            #Cria nova secção Historic
            dictHistoric  = {"Historic": []}

            for elem in employeesHistoric:

                dictHistoric["Historic"].append(elem[1])

    #Atualiza o counteudo JSON final
    dictFile.update(dictHistoric)

    return dicFile

try:
    con = cx_Oracle.connect(user, pwd, '{}:{}/{}'.format(host,portno,service_name))
    print('Successful connection!')

    cursor = con.cursor()

    query = "SELECT * FROM DEPARTMENTS"
    cursor.execute(query)

    result = cursor.fetchall()

    #Percorre todos os departamentos e para cada um constroi um ficheiro em JSON
    for department in result:

        #Atributos do departamento
        IdDepartment = department[0]
        departmentName = department[1]
        managerID = department[2]
        locationID = department[3]

        #Dicionário que vai conter o conteudo JSON que depois será escrito no ficheiro
        dictFile = {}

        #Criação do nome do ficheiro a ser utilizado
        fileName = "Department-" + departmentName + "-" + str(IdDepartment) + ".json"

        # Ficheiro JSON
        jsonDepartment = open(fileName, "w")

        #Caso o departamento tenha uma localização atribuida
        if locationID != None:
            dictFile = buildDepartmentLocation(dictFile)
        
        #Caso o departamento tenha um manager atribuido 
        if managerID != None:
           dictFile = buildDepartmentManger(dictFile)

        #Selecionar os empregos e employees associados ao departamento 
        dictFile = buildDepartmentJobs(dictFile)

        #Selecionar o histórico associado ao departamento 
        dictFile = buildDepartmentHistoric(dictFile)

        #Retira do conteudo JSON os campos a null
        dictFile = deleteNullFromDict(dictFile)
        
        #Escreve o conteudo JSON (dicionário) no ficheiro
        json.dump(dictFile, jsonDepartment, indent = 6,default=str) 
        jsonDepartment.close()

except cx_Oracle.DatabaseError as er:
    print('There is an error in the Oracle database:', er)