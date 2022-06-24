import cx_Oracle
import json
#from bson import json_util #Poderá ser necessário por causa de problema relacionado com a serialização do DateTime

# --------------------------------------------------------------------------------------------------------------------------- #

# ** FILIPA: Usar python do windows **

#-> Download da Pasta: https://www.oracle.com/pt/database/technologies/instant-client/winx64-64-downloads.html
#-> pip install cx-Oracle
#-> Código a utilizar para a conexão c/ caminho para a pasta: cx_Oracle.init_oracle_client(lib_dir=r"C:\Users\Filipa Pereira\Desktop\instantclient_21_3")

# ** LUISA: Usar python do Anaconda **
#-> Download da Pasta: https://www.oracle.com/pt/database/technologies/instant-client/winx64-64-downloads.html
#-> pip install cx-Oracle
#-> Copiar todos ficheiros *.dll da pasta descarregada para a pasta do Anaconda que contém o python.exe 

# --------------------------------------------------------------------------------------------------------------------------- #

# Se for Filipa descomentar 
cx_Oracle.init_oracle_client(lib_dir=r"C:\Users\Filipa Pereira\Desktop\instantclient_21_3")

user= 'HR'
pwd = 'hr2022'
host = 'localhost' 
service_name = 'orclpdb1.localdomain'
portno = 1521 

#######NOTAS#################
# Usei a variável count pois se quisermos meter varias queries no Neo4j ao mesmo tempo estas nao podem ter a mesma variável para a label


# Criar nodos das regiões
def createRegionNodes(cursor,cyph):

    query = "SELECT * FROM REGIONS"
    cursor.execute(query)
    result = cursor.fetchall()

    ## percorrer as rows da tabela Regions
    count = 1
    for r in result:
        id_r = r[0]
        name = r[1]
        cyph.write('CREATE (r'+ str (count) + ':Region {id:' + str(id_r) + ', name:' + "'" + name + "'"+  '});'+'\n')
        count += 1

    cyph.write('\n\n')    


# Criar nodos dos países
def createCountryNodes(cursor,cyph):

    query = "SELECT * FROM COUNTRIES"
    cursor.execute(query)
    result = cursor.fetchall()

    ## percorrer as rows da tabela Country
    count = 1
    for r in result:
        id_c = r[0]
        name = r[1]
        cyph.write('CREATE (c' +str(count) + ':Country {id:' +  "'" + id_c + "'" + ', name:' + "'"+ name + "'" + '});'+'\n')
        count += 1

    cyph.write('\n\n') 

# Criar nodos das localizações
def createLocationNodes(cursor,cyph):

    query = "SELECT * FROM LOCATIONS"
    cursor.execute(query)
    result = cursor.fetchall()
  
    count = 1
    # Percorrer as rows da tabela Locations
    for r in result:
        id_l = r[0]
        street = r[1]
        post_code = r[2]
        city = r[3]
        state_prov = r[4]
          
       # Aqui vou verificar quais dos campos estão nulos. Se estiverem então nao sao colocados
        if post_code and state_prov:
            cyph.write('CREATE (l'+ str(count) +':Location {id:' + str(id_l) + ', street:' + "'" + street + "'" + ', postalCode:' + "'" + post_code + "'" + ', city:' +  "'" + city +  "'" + ', stateProv:'+ "'" +state_prov + "'" +'});'+'\n')

        elif state_prov and not post_code:
            cyph.write('CREATE (l'+ str(count) +':Location {id:' + str(id_l) + ', street:' + "'" + street + "'" + ', city:' +  "'" + city +  "'" + ', stateProv:'+ "'" +state_prov + "'" +'});'+'\n')

        elif post_code and not state_prov:
            cyph.write('CREATE (l'+ str(count) +':Location {id:' + str(id_l) + ', street:' + "'" + street + "'" + ', postalCode:' + "'" + post_code + "'" + ', city:' +  "'" + city +  "'" + '});'+'\n')

        elif not post_code and not state_prov:
            cyph.write('CREATE (l'+ str(count) +':Location {id:' + str(id_l) + ', street:' + "'" + street + "'" + ', city:' +  "'" + city +  "'" + '});'+'\n')
        count += 1
        
    cyph.write('\n\n') 


#Criar nodos dos departamentos
def createDepartmentNodes(cursor,cyph):
    query = "SELECT * FROM DEPARTMENTS"
    cursor.execute(query)
    result = cursor.fetchall()

    count = 1
    for r in result:
        dep_id = r[0]
        dep_name = r[1]
        cyph.write('CREATE (d' +str(count) + ':Department {id:' + str(dep_id)  + ', name:' + "'"+ dep_name + "'" + '});'+'\n')
        count += 1
    cyph.write('\n\n') 


#Criar nodos dos employees
def createEmployeeNodes(cursor,cyph):
    query = "SELECT * FROM EMPLOYEES"
    cursor.execute(query)
    result = cursor.fetchall()

    queryD = "SELECT MANAGER_ID FROM DEPARTMENTS"
    cursor.execute(queryD)
    resultD = cursor.fetchall()
    
    # Obter managers dos vários departamentos
    managers = []
    for res in resultD:
        if res[0]:
            managers.append(res[0])


    #print(managers)
    count = 1
    for r in result:
        emp_id = r[0]
        first_name = r[1]
        last_name = r[2]
        email = r[3]
        phone = r[4]
        date_hired = r[5].strftime("%Y-%m-%d")
        salary = r[7]
        commission = r[8]
        is_manager = 0

        if emp_id in managers:
            is_manager = 1

        if commission:
            cyph.write('CREATE (e' + str(count) + ':Employee {id:' + str(emp_id)  + ', first_name:' + "'"+ first_name + "'" + ', last_name:' + "'"+ last_name + "'" + ',email:' + "'"+ email + "'"+ ',phone_number:'+ "'"+ phone + "'"+  ', date_hired: date(' + "'" + date_hired + "'" + '), salary:' + str(salary) + ',commission:' + str(commission) + ', is_manager:' + str(is_manager) + '});'+'\n')
        if not commission:
            cyph.write('CREATE (e' + str(count) + ':Employee {id:' + str(emp_id)  + ', first_name:' + "'"+ first_name + "'" + ', last_name:' + "'"+ last_name + "'" + ',email:' + "'"+ email + "'"+ ',phone_number:'+ "'"+ phone + "'"+  ', date_hired:date(' + "'" + date_hired + "'" + '), salary:' + str(salary) + ', is_manager:' + str(is_manager) + '});'+'\n')


        count += 1
    cyph.write('\n\n') 





#Criar nodos dos jobs
def createJobNodes(cursor,cyph):
    query = "SELECT * FROM JOBS"
    cursor.execute(query)
    result = cursor.fetchall()

    count = 1
    for r in result:
        job_id = r[0]
        job_title = r[1]
        min_sal = r[2]
        max_sal = r[3]
        cyph.write('CREATE (j' +str(count) + ':Job {id:'+ "'" + job_id + "'"  + ', title:' + "'"+ job_title + "'" + ',min_salary:' + str(min_sal) + ',max_sal:'+str(max_sal)+ '});'+'\n')
        count += 1
    cyph.write('\n\n') 

       
 
# função que cria a relação (location -> region) e ( Country -> region)
def create_country_rel(cursor,cyph):

    queryL = "SELECT * FROM LOCATIONS"
    
    cursor.execute(queryL)
    resultL = cursor.fetchall()
    
    count = 1
    #cyph.write("WITH 1 AS DUMMY\n")
    # relação (location -> country)
    for r in resultL:
        loc_id = r[0]
        country_id = r[5]
        var_loc = 'l' + str(count)
        var_c = 'c' + str(count)
        query = ('MATCH (' + var_loc + ':Location{id:' + str(loc_id) + '}), ' 
                 '(' + var_c + ':Country {id:'+ "'" +country_id+ "'" + '}) '
                 'CREATE ('+ var_loc +')-[:Has_Country]->(' + var_c + ');\n') 
                
        cyph.write(query)
        count += 1

    cyph.write('\n\n') 


def create_region_rel(cursor,cyph):
    # relação (country -> region)
    queryC = "SELECT * FROM COUNTRIES"
    
    cursor.execute(queryC)
    resultC = cursor.fetchall()
    count = 1
    for r in resultC:
        c_id = r[0]
        reg_id = r[2]
        var_c = 'c' + str(count)
        var_reg = 'r' + str(count)
        query = ('MATCH (' + var_c + ':Country{id:' + "'" + c_id + "'" + '}), ' 
                 '(' + var_reg + ':Region{id:'+ str(reg_id) + '}) '
                 'CREATE ('+ var_c +')-[:Has_Region]->(' + var_reg + ');\n') 
                
        cyph.write(query)
        count += 1
    cyph.write('\n\n')


def create_dep_rel(cursor,cyph):

    queryD = "SELECT * FROM DEPARTMENTS"
    cursor.execute(queryD)
    resultD = cursor.fetchall()

    count = 1
    for r in resultD:
        dep_id = r[0]
        loc_id = r[3]
        var_d = 'd' + str(count)
        var_l = 'l' + str(count)

        query = ('MATCH (' + var_d + ':Department{id:' + str(dep_id) + '}), ' 
                 '(' + var_l + ':Location{id:'+ str(loc_id) + '}) '
                 'CREATE ('+ var_d +')-[:Has_Location]->(' + var_l + ');\n') 
                
        cyph.write(query)
        count += 1
    cyph.write('\n\n')


def create_job_rel(cursor,cyph):
    queryE = "SELECT * FROM EMPLOYEES"
    cursor.execute(queryE)
    resultE = cursor.fetchall()

    count = 1
    for r in resultE:
        emp_id = r[0]
        job_id = r[6]
        var_e = 'e' + str(count)
        var_j = 'j' + str(count)

        query = ('MATCH (' + var_e + ':Employee{id:' + str(emp_id) + '}), ' 
                 '(' + var_j + ':Job{id:'+ "'" +job_id + "'" + '}) '
                 'CREATE ('+ var_e +')-[:Has_Job]->(' + var_j + ');\n') 
                
        cyph.write(query)
        count += 1
    cyph.write('\n\n')


def create_emp_dep_rel(cursor,cyph):
    queryE = "SELECT * FROM EMPLOYEES"
    cursor.execute(queryE)
    resultE = cursor.fetchall()

    count = 1
    for r in resultE:
        emp_id = r[0]
        dep_id = r[10]
        var_e = 'e' + str(count)
        var_d = 'd' + str(count)

        if dep_id:
            query = ('MATCH (' + var_e + ':Employee{id:' + str(emp_id) + '}), ' 
                    '(' + var_d + ':Department{id:'+ str(dep_id) + '}) '
                    'CREATE ('+ var_e +')-[:Works]->(' + var_d + ');\n') 
                    
            cyph.write(query)
            count += 1
    cyph.write('\n\n')



def create_emp_dep_history_rel(cursor,cyph):
    query = "SELECT * FROM JOB_HISTORY"
    cursor.execute(query)
    result = cursor.fetchall()

    count = 1
    for r in result:
        emp_id = r[0]
        start_date = r[1].strftime("%Y-%m-%d")
        end_date = r[2].strftime("%Y-%m-%d")
        job_id = r[3]
        dep_id = r[4]
        var_e = 'e' + str(count)
        var_d = 'd' + str(count)

        query_aux = "SELECT JOB_TITLE FROM JOBS WHERE JOB_ID = " + "'" + job_id + "'"
        cursor.execute(query_aux)
        res_aux = cursor.fetchall()
        job_name =  res_aux[0][0]

        query = ('MATCH (' + var_e + ':Employee{id:' + str(emp_id) + '}), ' 
                    '(' + var_d + ':Department{id:'+ str(dep_id) + '}) '
                    'CREATE ('+ var_e +')-[:Worked { job_name:' + "'" + job_name + "'" + ',start_date: date(' + "'" + start_date + "'" + '),end_date: date(' + "'" + end_date + "'" ')}]->(' + var_d + ');\n') 
                    
        cyph.write(query)
        count += 1

    cyph.write('\n\n')
    

def remove_ids(cyph):
    
    query = "MATCH (r:Region) REMOVE r.id;"  + "\n"             
    cyph.write(query)
    query = "MATCH (c:Country) REMOVE c.id;"  + "\n"              
    cyph.write(query)
    query = "MATCH (l:Location) REMOVE l.id;"  + "\n"              
    cyph.write(query)
    query = "MATCH (d:Department) REMOVE d.id;"  + "\n"              
    cyph.write(query)
    query = "MATCH (e:Employee) REMOVE e.id;"  + "\n"              
    cyph.write(query)
    query = "MATCH (j:Job) REMOVE j.id;"  + "\n"              
    cyph.write(query)
    
        


try:
    con = cx_Oracle.connect(user, pwd, '{}:{}/{}'.format(host,portno,service_name))
    print('Sucessful connection!')

    cursor = con.cursor()
    
    #Criação do nome do ficheiro a ser utilizado
    fileName = "Script" + ".txt"

    # Ficheiro 
    cyph = open(fileName, "w")

    ### Criar nodos Region , Country e Location ###
    createRegionNodes(cursor,cyph)
    createCountryNodes(cursor,cyph)
    createLocationNodes(cursor,cyph)
    createDepartmentNodes(cursor,cyph)
    createEmployeeNodes(cursor,cyph)
    createJobNodes(cursor,cyph)

 
    # Criar relação entre location e Country
    create_country_rel(cursor,cyph)

    # Criar relação entre Country e Region
    create_region_rel(cursor,cyph)

     # Criar relação entre Departament e Location
    create_dep_rel(cursor,cyph)
    
    # Criar relação Employee e Job
    create_job_rel(cursor,cyph)

    # Criar relação Employee e Department onde trabalha atualmente
    create_emp_dep_rel(cursor,cyph)

    # Criar relação Employee e Department Onde trabalhou na passado (job history)
    create_emp_dep_history_rel(cursor,cyph)

    #Remover os ids dos nodos
    remove_ids(cyph)
    

except cx_Oracle.DatabaseError as er:
    print('There is an error in the Oracle database:', er)