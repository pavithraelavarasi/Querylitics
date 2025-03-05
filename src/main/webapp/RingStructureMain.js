class CassandraRing {
    constructor() {
      this.nodes = Array.from({ length: 6 }, (_, i) => ({
        id: i + 1,
        token: Math.floor((i * 360) / 6),
        isActive: true,
        data: []
      }));
      
      this.data = [];
      this.replicationFactor = 2;
      
      this.initializeUI();
      this.render();
    }
  
    initializeUI() {
      document.getElementById('data-form').addEventListener('submit', (e) => {
        e.preventDefault();
        const input = document.getElementById('key-input');
        const key = input.value.trim();
        if (key) {
          this.addData(key);
          input.value = '';
        }
      });
  
      document.getElementById('replication-factor').addEventListener('input', (e) => {
        this.replicationFactor = parseInt(e.target.value);
        document.getElementById('rf-value').textContent = this.replicationFactor;
      });
  
      document.getElementById('reset-button').addEventListener('click', () => {
        this.reset();
      });
    }
  
    createNodeElement(node, position) {
      const nodeEl = document.createElement('div');
      nodeEl.className = `node ${node.isActive ? 'active' : 'inactive'}`;
      nodeEl.style.left = `${position.x}px`;
      nodeEl.style.top = `${position.y}px`;
      
      nodeEl.innerHTML = `
        <div class="node-tooltip">
          <div class="node-tooltip-title">Node ${node.id} Data</div>
          <div class="node-tooltip-data">
            ${node.data.length ? node.data.map(key => `<div>${key}</div>`).join('') : ''}
          </div>
        </div>
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="2" y="2" width="20" height="8" rx="2" ry="2"></rect>
          <rect x="2" y="14" width="20" height="8" rx="2" ry="2"></rect>
          <line x1="6" y1="6" x2="6" y2="6"></line>
          <line x1="6" y1="18" x2="6" y2="18"></line>
        </svg>
        <div class="node-label">
          Node ${node.id}
          <div class="node-token">${node.token}°</div>
        </div>
      `;
  
      nodeEl.addEventListener('click', () => {
        this.toggleNode(node.id);
      });
  
      return nodeEl;
    }
  
    addData(key) {
      const hash = simpleHash(key);
      const primaryNode = getResponsibleNode(hash, this.nodes);
      const replicaNodes = getReplicaNodes(primaryNode, this.nodes, this.replicationFactor);
      
      const newDataItem = {
        key,
        hash,
        replicas: replicaNodes.map(n => n.id)
      };
  
      this.data.push(newDataItem);
      
      this.nodes = this.nodes.map(node => ({
        ...node,
        data: replicaNodes.map(n => n.id).includes(node.id) 
          ? [...node.data, key]
          : node.data
      }));
  
      this.render();
      this.animateDataInsertion(key, primaryNode);
    }
  
    animateDataInsertion(key, primaryNode) {
      const dataEl = document.createElement('div');
      dataEl.className = 'data-item initial';
      dataEl.textContent = key;

      const dataContainer = document.getElementById('data-container');
      dataContainer.appendChild(dataEl);

      // Force reflow
      dataEl.offsetHeight;

      // Appear in center with glow effect
      dataEl.classList.remove('initial');
      dataEl.classList.add('appear', 'glow-effect');

      // Move to primary node with ring lighting
      const primaryIndex = this.nodes.findIndex(n => n.id === primaryNode.id);
      const primaryPos = calculateNodePosition(primaryIndex, this.nodes.length, 250);

      setTimeout(() => {
        dataEl.classList.add('move-to-node');
        dataEl.style.left = `${primaryPos.x}px`;
        dataEl.style.top = `${primaryPos.y}px`;

        // Highlight primary node (no glow, just existing pulse)
        const primaryNodeEl = document.querySelector(`.node:nth-child(${primaryNode.id})`);
        primaryNodeEl.classList.add('highlight');

        // Add ring lighting effect
        const ring = document.querySelector('.ring-circle');
        ring.classList.add('ring-lighting');
      }, 600);

      // Get replication nodes
      const replicaNodes = getReplicaNodes(primaryNode, this.nodes, this.replicationFactor);
      const replicaNodeIds = replicaNodes.map(n => n.id).filter(id => id !== primaryNode.id);

      // Animate replication sequentially with lighting effects
      let delay = 1200;
      replicaNodeIds.forEach((replicaId, index) => {
        setTimeout(() => {
          // Clone data item for replication with glowing trail
          const replicaEl = dataEl.cloneNode(true);
          replicaEl.className = 'data-item replicate glow-trail';
          replicaEl.style.left = `${primaryPos.x}px`;
          replicaEl.style.top = `${primaryPos.y}px`;
          dataContainer.appendChild(replicaEl);

          // Calculate replica node position
          const replicaIndex = this.nodes.findIndex(n => n.id === replicaId);
          const replicaPos = calculateNodePosition(replicaIndex, this.nodes.length, 250);

          // Highlight replica node (no glow, just existing pulse)
          const replicaNodeEl = document.querySelector(`.node:nth-child(${replicaId})`);
          replicaNodeEl.classList.add('replication-target');

          // Move to replica node with dynamic shadow
          setTimeout(() => {
            replicaEl.style.left = `${replicaPos.x}px`;
            replicaEl.style.top = `${replicaPos.y}px`;
            replicaEl.classList.add('dynamic-shadow');

            // Fade out glow
            setTimeout(() => {
              replicaEl.classList.remove('glow-trail');
              replicaEl.classList.add('replicated');
            }, 600);
          }, 50);

          // Clean up replica element
          setTimeout(() => {
            replicaEl.remove();
          }, 1200);
        }, delay + index * 800);
      });

      // Clean up primary data item and effects
      setTimeout(() => {
        dataEl.classList.add('replicate');
        const primaryNodeEl = document.querySelector(`.node:nth-child(${primaryNode.id})`);
        primaryNodeEl.classList.remove('highlight');
        const ring = document.querySelector('.ring-circle');
        ring.classList.remove('ring-lighting');

        setTimeout(() => {
          dataEl.remove();
        }, 300);
      }, delay + replicaNodeIds.length * 800);
    }
  
    toggleNode(nodeId) {
      this.nodes = this.nodes.map(node =>
        node.id === nodeId ? { ...node, isActive: !node.isActive } : node
      );
      this.render();
    }
  
    reset() {
      this.nodes = Array.from({ length: 6 }, (_, i) => ({
        id: i + 1,
        token: Math.floor((i * 360) / 6),
        isActive: true,
        data: []
      }));
      this.data = [];
      this.render();
    }
  
    render() {
      const nodesContainer = document.getElementById('nodes-container');
      nodesContainer.innerHTML = '';
      
      this.nodes.forEach((node, index) => {
        const position = calculateNodePosition(index, this.nodes.length, 250);
        const nodeEl = this.createNodeElement(node, position);
        nodesContainer.appendChild(nodeEl);
      });
  
      const nodeGrid = document.getElementById('node-data-grid');
      nodeGrid.innerHTML = '';
      
      this.nodes.forEach(node => {
        const nodeDataEl = document.createElement('div');
        nodeDataEl.className = `node-data ${node.isActive ? 'active' : 'inactive'}`;
        
        nodeDataEl.innerHTML = `
          <h3>Node ${node.id}</h3>
          <div class="node-data-items">
            ${node.data.map(key => `<div>${key}</div>`).join('')}
          </div>
        `;
        
        nodeGrid.appendChild(nodeDataEl);
      });
    }
}

// Helper functions
function simpleHash(key) {
  let hash = 0;
  for (let i = 0; i < key.length; i++) {
    hash = ((hash << 5) - hash) + key.charCodeAt(i);
    hash = hash & hash;
  }
  return Math.abs(hash % 360);
}

function getResponsibleNode(hash, nodes) {
  const activeNodes = nodes.filter(n => n.isActive);
  return activeNodes.reduce((closest, node) => {
    if (hash <= node.token && (closest.token > node.token || hash > closest.token)) {
      return node;
    }
    return closest;
  }, activeNodes[0]);
}

function getReplicaNodes(primaryNode, nodes, replicationFactor) {
  const activeNodes = nodes.filter(n => n.isActive);
  const replicas = [primaryNode];
  let currentIndex = activeNodes.findIndex(n => n.id === primaryNode.id);

  while (replicas.length < replicationFactor && replicas.length < activeNodes.length) {
    currentIndex = (currentIndex + 1) % activeNodes.length;
    replicas.push(activeNodes[currentIndex]);
  }

  return replicas;
}

function calculateNodePosition(index, total, radius) {
  const angle = (index * 2 * Math.PI) / total - Math.PI / 2;
  return {
    x: radius * Math.cos(angle) + 300,
    y: radius * Math.sin(angle) + 300
  };
}

// Initialize the application
document.addEventListener('DOMContentLoaded', () => {
  new CassandraRing();
});