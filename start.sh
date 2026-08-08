#!/bin/bash
# Nawala Gateway Platform - Quick Start Script

set -e

echo "========================================"
echo "  Nawala Gateway Platform"
echo "  Open Source API Management"
echo "========================================"
echo ""

# Check Docker
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "Error: Docker Compose is not installed"
    exit 1
fi

# Determine compose command
if docker compose version &> /dev/null; then
    COMPOSE="docker compose"
else
    COMPOSE="docker-compose"
fi

case "$1" in
    start)
        echo "Starting Nawala Gateway Platform..."
        $COMPOSE up -d
        echo ""
        echo "Services starting..."
        echo "  Platform: http://localhost:8080"
        echo "  Gateway:  http://localhost:8081"
        echo ""
        echo "Default login:"
        echo "  Username: admin"
        echo "  Password: admin123"
        ;;
    stop)
        echo "Stopping Nawala Gateway Platform..."
        $COMPOSE down
        ;;
    restart)
        echo "Restarting Nawala Gateway Platform..."
        $COMPOSE restart
        ;;
    logs)
        $COMPOSE logs -f ${2:-}
        ;;
    status)
        $COMPOSE ps
        ;;
    clean)
        echo "Warning: This will delete all data!"
        read -p "Are you sure? (y/N) " confirm
        if [[ $confirm == [yY] ]]; then
            $COMPOSE down -v
            echo "All containers and volumes removed."
        fi
        ;;
    build)
        echo "Building images..."
        $COMPOSE build --no-cache
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|logs|status|clean|build}"
        echo ""
        echo "Commands:"
        echo "  start   - Start all services"
        echo "  stop    - Stop all services"
        echo "  restart - Restart all services"
        echo "  logs    - View logs (optionally specify service)"
        echo "  status  - Show service status"
        echo "  clean   - Remove all containers and data"
        echo "  build   - Rebuild Docker images"
        exit 1
        ;;
esac
