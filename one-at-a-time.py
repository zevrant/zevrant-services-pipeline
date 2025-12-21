import json
import os
import requests

MEALIE_BASE_URL = "https://mealie.zevrant-services.internal"
API_TOKEN = os.environ.get("MEALIE_TOKEN")

if not API_TOKEN:
    raise RuntimeError("MEALIE_API_TOKEN environment variable not set")

HEADERS = {
    "Authorization": f"Bearer {API_TOKEN}",
    "Content-Type": "application/json",
}

UPLOAD_ENDPOINT = f"{MEALIE_BASE_URL}/api/recipes"


def recipe_exisits(recipe_name) -> bool:
    response = requests.get(
        UPLOAD_ENDPOINT + '/' + recipe_name,
        headers=HEADERS,
    )
    print(response)
    if response.status_code == 404:
        return False

    return True


def create_recipe(recipe_name: str) -> bool:
    response = requests.post(
        UPLOAD_ENDPOINT,
        headers=HEADERS,
        json={"name": recipe_name},
        timeout=30,
    )

    if response.status_code in (200, 201):
        print(f"✅ Uploaded: {recipe.get('name')}")
        return True
    else:
        print(f"❌ Failed: {recipe.get('name')}")
        print(f"   Status: {response.status_code}")
        print(f"   Response: {response.text}")
        return False


def update_recipe(recipe: dict[str]) -> bool:
    response = requests.put(
        UPLOAD_ENDPOINT + '/' + recipe.get('name'),
        headers=HEADERS,
        json=recipe,
        timeout=30,
    )

    if response.status_code in (200, 201):
        print(f"✅ Uploaded: {recipe.get('name')}")
        return True
    else:
        print(f"❌ Failed: {recipe.get('name')}")
        print(f"   Status: {response.status_code}")
        print(f"   Response: {response.text}")
        return False


def main():
    recipes: list[dict]
    with open("batch1.json", "r", encoding="utf-8") as f:
        recipes = json.load(f)

    print(f"Uploading {len(recipes)} recipes...\n")

    success = 0
    already_created = 0
    for recipe in recipes:
        if recipe_exisits(recipe['name']):
            already_created += 1
        elif update_recipe(recipe):
            success += 1

    print(f"\nDone. {success}/{len(recipes)} recipes uploaded successfully.")
    print(f"\n {already_created} recipes skipped due to already existing.")


if __name__ == "__main__":
    main()
