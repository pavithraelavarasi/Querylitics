const calculateNodePosition = (index, total, radius) => {
    const angle = (index * 2 * Math.PI) / total - Math.PI / 2;
    return {
      x: radius * Math.cos(angle) + 300,
      y: radius * Math.sin(angle) + 300
    };
  };
  
  console.log("Hi");
  
  const simpleHash = (key) => {
    let hash = 0;
    for (let i = 0; i < key.length; i++) {
      hash = ((hash << 5) - hash) + key.charCodeAt(i);
      hash = hash & hash;
    }
    return Math.abs(hash % 360);
  };
  
  const getResponsibleNode = (hash, nodes) => {
    const activeNodes = nodes.filter(n => n.isActive);
    return activeNodes.reduce((closest, node) => {
      if (hash <= node.token && (closest.token > node.token || hash > closest.token)) {
        return node;
      }
      return closest;
    }, activeNodes[0]);
  };
  
  const getReplicaNodes = (primaryNode, nodes, rf) => {
    const activeNodes = nodes.filter(n => n.isActive);
    const replicas = [primaryNode];
    let currentIndex = activeNodes.findIndex(n => n.id === primaryNode.id);
  
    while (replicas.length < rf && replicas.length < activeNodes.length) {
      currentIndex = (currentIndex + 1) % activeNodes.length;
      replicas.push(activeNodes[currentIndex]);
    }
  
    return replicas;
  };