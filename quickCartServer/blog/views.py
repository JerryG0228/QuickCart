from django.utils import timezone
from .models import Post
from django.shortcuts import render, get_object_or_404, redirect
from .forms import Postform
from rest_framework import viewsets
from .serializers import PostSerializer
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt

class blogImage(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer


def post_list(request):
    posts = Post.objects.filter(published_date__lte=timezone.now()).order_by('published_date')
    return render(request, 'blog/post_list.html', {'posts': posts})


def post_detail(request, pk):
    post = get_object_or_404(Post, pk=pk)
    return render(request, 'blog/post_detail.html', {'post': post})


def post_new(request):
    if request.method == "POST":
        form = Postform(request.POST)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = request.user
            post.published_date = timezone.now()
            post.save()
            return redirect('post_detail', pk=post.pk)
    else:
        form = Postform()
    return render(request, 'blog/post_edit.html', {"form": form})


def post_edit(request, pk):
    post = get_object_or_404(Post, pk=pk)
    if request.method == "POST":
        form = Postform(request.POST, instance=post)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = request.user
            post.published_date = timezone.now()
            post.save()
            return redirect("post_detail", pk=post.pk)
    else:
        form = Postform(instance=post)
    return render(request, "blog/post_edit.html", {"form": form})

@csrf_exempt
def reset_database(request):
    if request.method == 'POST':
        try:
            # Post 모델의 모든 데이터를 삭제
            Post.objects.all().delete()
            return JsonResponse({"status": "success", "message": "Database reset complete."}, status=200)
        except Exception as e:
            return JsonResponse({"status": "error", "message": str(e)}, status=500)
    else:
        return JsonResponse({"status": "error", "message": "Invalid request method."}, status=405)