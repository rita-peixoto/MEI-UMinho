
sig Node { 
    link : set Node,                   // Arestas adjacentes
    var path : set Node,               // Caminho euleriano
    var visited : set Node             // Arestas visitadas     
}

var one sig NodeAtual, NodeInicial in Node {}           // Variável que guarda os nodos atual e inicial


// Especificar as condições iniciais do sistema 
fact init {
    no path
    no visited

    one n : Node | NodeAtual = n and NodeInicial = n
    all n : Node | n->(Node-n) in link and (Node-n)->n in link
    all n : Node | n->n not in link
}


pred eulerian_circuit[n , n1: Node] {
    //Guards 
    n != n1
    n1 not in n.visited //a aresta n->n1 ainda não foi visitada
    n not in n1.visited //a aresta n1->n ainda não foi visitada
    n1 in n.link //existe uma aresta de n->n1
    NodeAtual = n

    //Effects
    n.path' = n.path + n1
    n.visited' = n.visited + n1 //A aresta n->n1 passa a estar visitada
    n1.visited' = n1.visited + n
    NodeAtual' = n1
    
    //Frame conditions 
    all n2 : Node - n | n2.path' = n2.path //todos os outros nodos ficam iguais
    all n2 : Node - n - n1 | n2.visited' = n2.visited
    NodeInicial' = NodeInicial
}

pred nop {
    //Guards
    visited = link
    NodeAtual = NodeInicial

    //Frame conditions
    path' = path
    visited' = visited
    NodeInicial' = NodeInicial
    NodeAtual' = NodeAtual
}

fact Events {
    always (nop or 
            some n : Node , n1 : n.link | eulerian_circuit[n, n1])
}


// run que exemplifica com um grafo completo com 5 nodos
run Exemplo { 
    all n : Node | n.link = (Node-n)
} for exactly 5 Node, 20 steps

