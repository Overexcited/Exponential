import os
import sys
from pathlib import Path


def start_backend(port: int = 5001):
    backend = Path(__file__).resolve().parent / "eigent-backend"
    if str(backend) not in sys.path:
        sys.path.insert(0, str(backend))

    home = Path.home()
    (home / ".eigent" / "models").mkdir(parents=True, exist_ok=True)
    (home / ".eigent" / "workspaces").mkdir(parents=True, exist_ok=True)

    os.environ.setdefault("EIGENT_BRAIN_HOST", "127.0.0.1")
    os.environ["EIGENT_BRAIN_PORT"] = str(port)
    os.environ.setdefault("ENVIRONMENT", "production")
    os.environ.setdefault("EIGENT_MOBILE", "true")
    os.environ.setdefault("EIGENT_OFFLINE_MODE", "true")
    os.environ.setdefault("LLAMA_CPP_API_HOST", "http://127.0.0.1:8080/v1")

    import main
    import uvicorn

    config = uvicorn.Config(
        main.api,
        host="127.0.0.1",
        port=port,
        log_level="info",
    )
    server = uvicorn.Server(config)
    server.install_signal_handlers = lambda: None
    server.run()
